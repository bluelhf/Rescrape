module blue.lhf.rescrape {
    exports blue.lhf.rescrape.api;
    exports blue.lhf.rescrape.api.search;
    exports blue.lhf.rescrape.api.query;

    exports blue.lhf.rescrape.util.query to argo;

    requires org.jetbrains.annotations;
    requires java.base;
    requires org.jsoup;
    requires argo;
}