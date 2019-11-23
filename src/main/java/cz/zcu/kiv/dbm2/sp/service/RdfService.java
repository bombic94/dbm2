package cz.zcu.kiv.dbm2.sp.service;

import cz.zcu.kiv.dbm2.sp.model.RdfFormat;
import cz.zcu.kiv.dbm2.sp.model.RdfPredicate;
import cz.zcu.kiv.dbm2.sp.model.RdfType;
import cz.zcu.kiv.dbm2.sp.model.SelectedPredicate;
import cz.zcu.kiv.dbm2.sp.util.Constants;
import cz.zcu.kiv.dbm2.sp.util.Utils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.ResourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
public class RdfService {

    private Model model;
    private Model renamedModel;
    private List<SelectedPredicate> selectedPredicates;
    private List<RdfType> sortedRdfTypes;
    private RdfFormat rdfFormat;

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

        //add to set, so there are no duplicates
        Set<RdfType> rdfTypes = new HashSet<>();
        for (Resource resource : model.listSubjects().toList()) {
            for (Statement statement : resource.listProperties().toList()) {
                Property predicate = statement.getPredicate();
                RDFNode object = statement.getObject();
                if (predicate.getURI().equals(Constants.RDF_TYPE)) {
                    RdfType type = new RdfType(resource, object.toString());
                    type.generateProperties();
                    rdfTypes.add(type);
                }
            }
        }
        sortedRdfTypes = new ArrayList<>(rdfTypes);
        Collections.sort(sortedRdfTypes);
        return sortedRdfTypes;
    }

    /**
     * Method for model renaming. Create copy of original model and for each subject
     * @param includeOrigId
     * @param selectedProperties
     * @return
     * @throws IOException
     */
    public String renameModel(String[] includeOrigId, String[] selectedProperties) throws IOException {
        //create clear model
        renamedModel = ModelFactory.createDefaultModel().add(model);
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

            String resourceTypeString = resource.getProperty(renamedModel.getProperty(Constants.RDF_TYPE)).getObject().toString();
            //for each predicate in subject
            for (Statement statement : resource.listProperties().toList()) {
                //for each selected predicate for renaming
                for (SelectedPredicate selectedPredicate : selectedPredicates) {

                    //need to check if resource type is one of selected types and selected predicate is same as predicate
                    if (statement.getPredicate().getLocalName().equals(selectedPredicate.getPredicate().getLocalName())
                            && Utils.getLastPartFromURI(resourceTypeString).equals(selectedPredicate.getType().getTypeName())) {
                        //append predicate name and object name to subject new name
                        sb.append(selectedPredicate.getPredicate().getLocalName())
                                .append("-")
                                .append(Utils.getLastPartFromURI(statement.getObject().toString()))
                                .append("-");
                        rename = true;
                    }
                    //if includeOrigId is checked for this subject type, add original resource ID
                    if (includeOrigId != null && includeOrigId.length > 0) {
                        for (String s : includeOrigId) {
                            if (s.equals(selectedPredicate.getType().getTypeName())) {
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
                ResourceUtils.renameResource(resource, sb.toString());
            }
        }
        if (!isRenamedModelOK()) {
            return "Nodes in model do not have unique name! Select different properties for renaming";
        }
        return "OK";
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
        System.out.println(modelResources.size() + " " + renamedModelResources.size());

        if (modelResources.size() != renamedModelResources.size()) return false;
        int modelResourcesTriplesCount = 0;
        int renamedModelResourcesTriplesCount = 0;
        for (int i = 0; i < modelResources.size(); i++) {
            modelResourcesTriplesCount += modelResources.get(i).listProperties().toList().size();
            renamedModelResourcesTriplesCount += renamedModelResources.get(i).listProperties().toList().size();
        }
        System.out.println(modelResourcesTriplesCount + " " + renamedModelResourcesTriplesCount);
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
}