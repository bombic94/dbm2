package cz.zcu.kiv.dbm2.sp.model;

import java.util.UUID;

public class RdfPredicate implements Comparable {

    private RdfType rdfType;
    private boolean selected;
    private String localName;
    private String uri;
    private String id;

    public RdfPredicate(RdfType rdfType, String localName, String uri, boolean selected) {
        this.rdfType = rdfType;
        this.id = UUID.randomUUID().toString();
        this.localName = localName;
        this.uri = uri;
        this.selected = selected;
    }

    public RdfType getRdfType() {
        return rdfType;
    }

    public void setRdfType(RdfType rdfType) {
        this.rdfType = rdfType;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
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
        return (o instanceof RdfPredicate) && (((RdfPredicate) o).getLocalName()).equals(this.getLocalName());
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof RdfPredicate) {
            return this.getLocalName().compareTo(((RdfPredicate) o).getLocalName());
        }
        return 0;
    }
}
