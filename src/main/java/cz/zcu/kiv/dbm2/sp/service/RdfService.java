package cz.zcu.kiv.dbm2.sp.service;

import cz.zcu.kiv.dbm2.sp.model.RdfFormat;
import cz.zcu.kiv.dbm2.sp.model.RdfPredicate;
import cz.zcu.kiv.dbm2.sp.model.RdfType;
import cz.zcu.kiv.dbm2.sp.model.SelectedPredicate;
import cz.zcu.kiv.dbm2.sp.util.Utils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.InfGraph;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static org.apache.jena.rdfxml.xmlinput.impl.Names.RDF_TYPE;


@Service
public class RdfService {

    private Model model;
    private Model renamedModel;
    private List<SelectedPredicate> selectedPredicates;
    private List<RdfType> sortedRdfTypes;
    private RdfFormat rdfFormat;

    HashMap<String, List<String>> renamingMap;

    /**
     * Create RDF model from input file
     * @param file input file in ttl, nt or xml format
     * @return RDF Model
     * @throws IOException
     */
    public Model processModelFromFile(MultipartFile file) throws IOException {
        InputStream is = file.getInputStream();
        Model model = ModelFactory.createDefaultModel();

        //find and save file format
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        rdfFormat = RdfFormat.valueOf(extension.toUpperCase());
        model.read(is, null, rdfFormat.getLang());

        return model;
    }

    /**
     * Generate list of types that are included in model
     * @param file input file in ttl, nt or xml format
     * @return List of types with list of properties
     * @throws IOException
     */
    public List<RdfType> generateTypes(MultipartFile file) throws IOException {
        model = this.processModelFromFile(file);

        List<RdfType> types = new ArrayList<>();
        for (Resource resource : model.listSubjects().toList()) {
            String resourceTypeString = getResourceType(resource);
            if (resourceTypeString == null || resourceTypeString.isEmpty()) {
                continue;
            }
            RdfType type = new RdfType(Utils.getLastPartFromURI(resourceTypeString));

            for (Statement statement : resource.listProperties().toList()) {
                addPredicatesRecursively("", type, statement, 0);
            }
            types.add(type);
        }

        //group multiple predicates within one subject
        for (RdfType type : types) {
            Collections.sort(type.getProperties());
            type.groupPredicates();
        }

        //remove duplicates, if duplicate contains more predicates, add them to original
        sortedRdfTypes = new ArrayList<>();
        for (RdfType type : types) {
            if (!sortedRdfTypes.contains(type)) {
                sortedRdfTypes.add(type);
            } else {
                RdfType addedType = sortedRdfTypes.get(sortedRdfTypes.indexOf(type));
                for (RdfPredicate predicate : type.getProperties()) {
                    if (!addedType.getProperties().contains(predicate)) {
                        addedType.getProperties().add(predicate);
                    }
                }
            }
        }
        //sort types and predicates alphabetically
        for (RdfType type : sortedRdfTypes) {
            Collections.sort(type.getProperties());
        }
        Collections.sort(sortedRdfTypes);
        return sortedRdfTypes;
    }

    private void addPredicatesRecursively(String prefix, RdfType type, Statement statement, int depth) {
        //max depth 2, go deeper recursively
        if (depth <= 1 && statement.getObject().isResource()) {
            for (Statement st : statement.getObject().asResource().listProperties().toList()) {
                addPredicatesRecursively(prefix + statement.getPredicate().getLocalName() + ":", type, st, depth + 1);
            }
        }
        RdfPredicate predicate = new RdfPredicate(prefix + statement.getPredicate().getLocalName(),
                Utils.getFormattedObjectName(statement.getObject().toString()), false, false);

        type.addProperty(predicate);

    }

    /**
     * Method for model renaming. Create copy of original model and for each subject
     * @param includeOrigId
     * @param selectedProperties
     * @return
     * @throws IOException
     */
    public void renameModel(String[] includeOrigId, String[] selectedProperties) {
        //create clear model
        renamedModel = ModelFactory.createDefaultModel().add(model);
        //clear renaming map
        renamingMap = new HashMap<>();
        //mark selected for renaming
        markSelectedForRenaming(selectedProperties);

        //for each subject
        for (Resource resource : renamedModel.listSubjects().toList()) {
            boolean rename = false;
            boolean appendOrigId = false;
            String id = "";
            StringBuilder sb = new StringBuilder();
            String uriBase = Utils.getBaseFromURI(resource.getURI());
            sb.append(uriBase);

            String resourceTypeString = getResourceType(resource);
            if (resourceTypeString == null || resourceTypeString.isEmpty()) {
                continue;
            }
            //for each selected predicate for renaming
            for (SelectedPredicate selectedPredicate : selectedPredicates) {
                //for each predicate in subject
                for (Statement statement : resource.listProperties().toList()) {

                    String newObjectName = getNewObjectNameForRenaming(statement, selectedPredicate, resourceTypeString);
                    //need to check if resource type is one of selected types and selected predicate is same as predicate
                    if (newObjectName != null && !newObjectName.isEmpty()) {
                        //append predicate name and object name to subject new name
                        if (selectedPredicate.getPredicate().getMultiple() == true) {
                            if (sb.toString().contains(selectedPredicate.getPredicate().getExampleObject())) {
                                continue;
                            }
                            sb.append(selectedPredicate.getPredicate().getName())
                                    .append("-")
                                    .append(selectedPredicate.getPredicate().getExampleObject())
                                    .append("-");
                        } else {
                            sb.append(selectedPredicate.getPredicate().getName())
                                    .append("-")
                                    .append(Utils.getFormattedObjectName(newObjectName))
                                    .append("-");
                        }
                        rename = true;
                    }
                    //if includeOrigId is checked for this subject type, add original resource ID
                    if (includeOrigId != null && includeOrigId.length > 0) {
                        for (String s : includeOrigId) {
                            if (s.equals(selectedPredicate.getType().getName())) {
                                appendOrigId = true;
                                id = Utils.getLastPartFromURI(resource.getURI());
                            }
                        }
                    }
                }
            }
            //if this subject should be renamed, rename it.
            if (rename) {
                if (appendOrigId) {
                    sb.append(id);
                }
                else {
                    sb.setLength(sb.length() - 1);
                }
                renameResource(resource, sb.toString());
            }
        }
    }

    private String getNewObjectNameForRenaming(Statement statement, SelectedPredicate selectedPredicate, String resourceTypeString) {
        //types mismatch
        if (!Utils.getLastPartFromURI(resourceTypeString).equals(selectedPredicate.getType().getName())) return null;
        //selected predicate on 0 level
        if (statement.getPredicate().getLocalName().equals(selectedPredicate.getPredicate().getName())) return statement.getObject().toString();
        //selected predicate on 1 level
        if (statement.getObject().isResource()) {
            for (Statement statement1 : statement.getObject().asResource().listProperties().toList()) {
                String name = statement.getPredicate().getLocalName() + ":" + statement1.getPredicate().getLocalName();
                if (name.equals(selectedPredicate.getPredicate().getName())) return statement1.getObject().toString();
                //selected predicate on 2 level
                if (statement1.getObject().isResource()) {
                    for (Statement statement2 : statement1.getObject().asResource().listProperties().toList()) {
                        String name2 = name + ":" + statement2.getPredicate().getLocalName();
                        if (name2.equals(selectedPredicate.getPredicate().getName())) return statement.getObject().toString();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check if renamed model is the same as original model:
     * Number of subjects must be the same
     * Each subject must have same amount of triples as before
     * @return true, if model is OK, false otherwise
     */
    private boolean isRenamedModelOK() {
        List<Resource> modelResources = model.listSubjects().toList();
        List<Resource> renamedModelResources = renamedModel.listSubjects().toList();

        if (modelResources.size() != renamedModelResources.size()) return false;
        int modelResourcesTriplesCount = 0;
        int renamedModelResourcesTriplesCount = 0;
        for (int i = 0; i < modelResources.size(); i++) {
            modelResourcesTriplesCount += modelResources.get(i).listProperties().toList().size();
            renamedModelResourcesTriplesCount += renamedModelResources.get(i).listProperties().toList().size();
        }
        if(modelResourcesTriplesCount != renamedModelResourcesTriplesCount) {
            return false;
        }
        return true;
    }

    /**
     * Go through the list of selected properties and mark them as ready for renaming
     * @param selectedProperties list of names of properties
     */
    private void markSelectedForRenaming(String[] selectedProperties) {

        //set default all to false
        for (RdfType type : sortedRdfTypes) {
            for (RdfPredicate predicate : type.getProperties()) {
                predicate.setSelected(false);
            }
        }

        //add property to list of selected properties and also mark property as selected
        selectedPredicates = new ArrayList<>();
        if (selectedProperties != null && selectedProperties.length > 0) {
            for (RdfType type : sortedRdfTypes) {
                for (RdfPredicate predicate : type.getProperties()) {
                    for (String selectedProperty : selectedProperties) {
                        if (selectedProperty.equals(predicate.getId())) {
                            predicate.setSelected(true);
                            selectedPredicates.add(new SelectedPredicate(type, predicate));
                        }
                    }
                }
            }
        }
    }

    private String getResourceType(Resource resource) {
        Property typeURI = model.getProperty(RDF_TYPE.toString());
        List<Statement> statements = resource.listProperties(typeURI).toList();
        if (statements.size() > 1) {
            System.out.println(statements.size());
        }
        //no types
        if (statements.isEmpty()) {
            return null;
        }
        //first found type
        Statement statement = statements.get(0);
        Object object = statement.getObject();
        if (object == null) {
            return null;
        }
        return object.toString();
    }

    /**
     * Rename resource - method is the same as in ResourceUtils, with enhanced logging,
     * so user can confirm all changes before downloading the file
     * @param old old resource
     * @param uri new resource
     * @return
     */
    public Resource renameResource(Resource old, String uri) {

        String oldURI = old.getURI();
        if (oldURI != null && oldURI.equals(uri)) {
            return old;
        } else {
            Node resAsNode = old.asNode();
            Model model = old.getModel();
            Graph graph = model.getGraph();
            Graph rawGraph = graph instanceof InfGraph ? ((InfGraph)graph).getRawGraph() : graph;
            Resource newRes = model.createResource(uri);
            Node newResAsNode = newRes.asNode();
            boolean changeOccured = false;
            List<Triple> triples = new ArrayList(1000);
            boolean onFirstIterator = true;
            ExtendedIterator it = rawGraph.find(resAsNode, Node.ANY, Node.ANY);

            try {
                if (!it.hasNext()) {
                    it.close();
                    onFirstIterator = false;
                    it = rawGraph.find(Node.ANY, Node.ANY, resAsNode);
                }

                changeOccured = it.hasNext();

                while(it.hasNext()) {
                    for(int count = 0; it.hasNext() && count < 1000; ++count) {
                        triples.add((Triple) it.next());
                    }

                    it.close();
                    Iterator var14 = triples.iterator();

                    Triple t;
                    while(var14.hasNext()) {
                        t = (Triple)var14.next();
                        rawGraph.delete(t);
                    }

                    var14 = triples.iterator();

                    while(var14.hasNext()) {
                        t = (Triple)var14.next();
                        Node oldS = t.getSubject();
                        Node oldO = t.getObject();
                        Node newS = oldS.equals(resAsNode) ? newResAsNode : oldS;
                        Node newO = oldO.equals(resAsNode) ? newResAsNode : oldO;
                        rawGraph.add(Triple.create(newS, t.getPredicate(), newO));
                    }

                    triples.clear();
                    it = onFirstIterator ? rawGraph.find(resAsNode, Node.ANY, Node.ANY) : rawGraph.find(Node.ANY, Node.ANY, resAsNode);
                    if (onFirstIterator && !it.hasNext()) {
                        it.close();
                        onFirstIterator = false;
                        it = rawGraph.find(Node.ANY, Node.ANY, resAsNode);
                    }
                }
            } finally {
                it.close();
            }

            if (rawGraph != graph && changeOccured) {
                ((InfGraph)graph).rebind();
            }
            if(renamingMap.containsKey(uri)) {
                renamingMap.get(uri).add(oldURI);
            } else {
                renamingMap.put(uri, new ArrayList<>());
                renamingMap.get(uri).add(oldURI);
            }
            return newRes;
        }
    }

    public Model getModel() {
        return model;
    }

    public Model getRenamedModel() {
        return renamedModel;
    }

    public List<RdfType> getRdfTypes() {
        return sortedRdfTypes;
    }

    public RdfFormat getRdfFormat() {
        return rdfFormat;
    }

    public HashMap<String, List<String>> getRenamingMap() {
        return renamingMap;
    }
}
