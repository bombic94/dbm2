package cz.zcu.kiv.dbm2.sp.model;

import cz.zcu.kiv.dbm2.sp.util.Utils;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RdfType implements Comparable {

    private Resource resource;
    private List<RdfPredicate> properties;
    private String uriName;
    private String typeName;

    public RdfType(Resource resource, String uriName) {
        this.resource = resource;
        this.uriName = uriName;
        this.typeName = Utils.getLastPartFromURI(uriName);
        this.properties = new ArrayList<>();
    }

    /**
     * For each predicate in subject, generate object holding predicate info
     */
    public void generateProperties() {
        for (Statement statement : resource.listProperties().toList()) {
            RdfPredicate predicate = new RdfPredicate(this, statement.getPredicate().getLocalName(), statement.getPredicate().getURI(), false);
            properties.add(predicate);
        }
        Collections.sort(properties);
        groupPredicates();
    }

    /**
     * If subject contains multiple same predicates with different objects, group it to one predicate with count
     * of occurrences.
     */
    private void groupPredicates() {
        Map<String, Integer> itemCount = new HashMap<>();
        for (int i = 0; i < properties.size() - 1; i++) {
            for (int j = i + 1; j < properties.size(); j++) {
                if (properties.get(i).equals(properties.get(j))) {
                    String name = properties.get(i).getLocalName();
                    int count = itemCount.containsKey(name) ? itemCount.get(name) : 1; //the first one on position "i"
                    itemCount.put(name, count + 1); //the second one on position "j", and then +1 for each next found
                    i = j; //move i to next occurence
                }
            }
        }
        //change property with first occurrence
        for (Map.Entry item : itemCount.entrySet()) {
            for (RdfPredicate property : properties) {
                if (property.getLocalName().equals(item.getKey())) {
                    property.setLocalName(property.getLocalName() + "-count:" + item.getValue());
                    break;
                }
            }
        }
        //remove all the other occurrences
        properties.removeIf(i -> itemCount.containsKey(i.getLocalName()));
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public List<RdfPredicate> getProperties() {
        return properties;
    }

    public void setProperties(List<RdfPredicate> properties) {
        this.properties = properties;
    }

    public String getUriName() {
        return uriName;
    }

    public void setUriName(String uriName) {
        this.uriName = uriName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    /**
     * Equals based on typeName. This needs to be unique
     * @param o compared RdfType
     * @return true, if instances are equal
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof RdfType) && (((RdfType) o).getTypeName()).equals(this.getTypeName());
    }

    /**
     * Return uriName hashCode
     * @return uriName hashCode
     */
    @Override
    public int hashCode() {
        return uriName.hashCode();
    }

    /**
     * Compare based on typeName. This needs to be unique
     * @param o compared RdfType
     * @return comparison result
     */
    @Override
    public int compareTo(Object o) {
        if (o instanceof RdfType) {
            return this.getTypeName().compareTo(((RdfType) o).getTypeName());
        }
        return 0;
    }
}
