package cz.zcu.kiv.dbm2.sp.model;

public enum RdfFormat {

    TTL("ttl", "TURTLE", "application/x-turtle"),
    XML("xml", "RDF/XML", "application/xml"),
    NT("nt", "N-TRIPLE", " application/n-triples");

    private String extension;
    private String lang;
    private String contentType;

    RdfFormat(String extension, String lang, String contentType) {
        this.extension = extension;
        this.lang = lang;
        this.contentType = contentType;
    }

    public String getExtension() {
        return this.extension;
    }

    public String getLang() {
        return this.lang;
    }

    public String getContentType() { return this.contentType; }
}
