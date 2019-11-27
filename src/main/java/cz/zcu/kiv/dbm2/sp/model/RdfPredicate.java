package cz.zcu.kiv.dbm2.sp.model;

import java.util.UUID;

public class RdfPredicate implements Comparable {

    private String name;
    private String exampleObject;
    private boolean selected;
    private String id;
    private boolean multiple;

    public RdfPredicate(String name, String exampleObject, boolean selected, boolean multiple) {
        this.name = name;
        this.exampleObject = exampleObject;
        this.selected = selected;
        this.multiple = multiple;
        this.id = UUID.randomUUID().toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExampleObject() {
        return exampleObject;
    }

    public void setExampleObject(String exampleObject) {
        this.exampleObject = exampleObject;
    }

    public boolean getSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean getMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof RdfPredicate) && (((RdfPredicate) o).getName()).equals(this.getName());
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof RdfPredicate) {
            return this.getName().compareTo(((RdfPredicate) o).getName());
        }
        return 0;
    }
}
