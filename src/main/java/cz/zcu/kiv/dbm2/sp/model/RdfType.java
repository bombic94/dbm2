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

    private List<RdfPredicate> properties;
    private String name;

    public RdfType(String name) {
        this.name = name;
        this.properties = new ArrayList<>();
    }

    /**
     * For each predicate in subject, generate object holding predicate info
     */
//    public void generateProperties(Resource resource) {
//        for (Statement statement : resource.listProperties().toList()) {
//            RdfPredicate predicate = new RdfPredicate(statement.getPredicate().getLocalName(),
//                    Utils.getFormattedObjectName(statement.getObject().toString()), false);
//            properties.add(predicate);
//        }
//        Collections.sort(properties);
//        groupPredicates();
//    }

    /**
     * If subject contains multiple same predicates with different objects, group it to one predicate with count
     * of occurrences.
     */
    public void groupPredicates() {
        Map<String, Integer> itemCount = new HashMap<>();
        for (int i = 0; i < properties.size() - 1; i++) {
            for (int j = i + 1; j < properties.size(); j++) {
                if (properties.get(i).equals(properties.get(j))) {
                    String name = properties.get(i).getName();
                    int count = itemCount.containsKey(name) ? itemCount.get(name) : 1; //the first one on position "i"
                    itemCount.put(name, count + 1); //the second one on position "j", and then +1 for each next found
                    i = j; //move i to next occurence
                }
            }
        }
        //change property with first occurrence
        for (Map.Entry item : itemCount.entrySet()) {
            for (RdfPredicate property : properties) {
                if (property.getName().equals(item.getKey())) {
                    property.setExampleObject("count:" + item.getValue());
                    //property.setName(property.getName() + "-count:" + item.getValue());
                    property.setMultiple(true);
                    break;
                }
            }
        }
        //remove all the other occurrences
        properties.removeIf(i -> (itemCount.containsKey(i.getName()) && i.getMultiple() == false));
    }

    public List<RdfPredicate> getProperties() {
        return properties;
    }

    public void setProperties(List<RdfPredicate> properties) {
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Equals based on name. This needs to be unique
     * @param o compared RdfType
     * @return true, if instances are equal
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof RdfType) && (((RdfType) o).getName()).equals(this.getName());
    }

    /**
     * Return name hashCode
     * @return name hashCode
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Compare based on name. This needs to be unique
     * @param o compared RdfType
     * @return comparison result
     */
    @Override
    public int compareTo(Object o) {
        if (o instanceof RdfType) {
            return this.getName().compareTo(((RdfType) o).getName());
        }
        return 0;
    }

    public void addProperty(RdfPredicate predicate) {
        if (properties.contains(predicate)) {
            return;
        }
        properties.add(predicate);
    }

    public void addPropertyNoCheck(RdfPredicate predicate) {
        properties.add(predicate);
    }
}
