package cz.zcu.kiv.dbm2.sp.model;

public class SelectedPredicate {
    private RdfType type;
    private RdfPredicate predicate;

    public SelectedPredicate(RdfType type, RdfPredicate predicate) {
        this.type = type;
        this.predicate = predicate;
    }

    public RdfType getType() {
        return type;
    }

    public void setType(RdfType type) {
        this.type = type;
    }

    public RdfPredicate getPredicate() {
        return predicate;
    }

    public void setPredicate(RdfPredicate predicate) {
        this.predicate = predicate;
    }
}
