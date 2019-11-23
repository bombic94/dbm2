package cz.zcu.kiv.dbm2.sp.model;

import java.util.UUID;

public class RdfPredicate implements Comparable {

    private String name;
    private boolean selected;
    private String id;

    public RdfPredicate(String name, boolean selected) {
        this.name = name;
        this.selected = selected;
        this.id = UUID.randomUUID().toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
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
