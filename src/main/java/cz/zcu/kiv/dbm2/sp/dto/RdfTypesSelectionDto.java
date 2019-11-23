package cz.zcu.kiv.dbm2.sp.dto;

import cz.zcu.kiv.dbm2.sp.model.RdfType;

import java.util.ArrayList;
import java.util.List;

public class RdfTypesSelectionDto {

    private List<RdfType> types;

    public RdfTypesSelectionDto() {
        this.types = new ArrayList<>();
    }

    public List<RdfType> getTypes() {
        return types;
    }

    public void setTypes(List<RdfType> types) {
        this.types = types;
    }

    public void addType(RdfType type) {
        this.types.add(type);
    }
}
