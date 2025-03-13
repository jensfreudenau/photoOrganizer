module org.jens.importphotos {
    requires java.desktop;
    requires metadata.extractor;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    opens org.jens.importphotos to javafx.fxml;
    exports org.jens.importphotos;
}
