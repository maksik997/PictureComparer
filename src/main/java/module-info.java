module PictureComparer {
    requires com.github.benmanes.caffeine;
    requires JTransforms;
    requires org.jetbrains.annotations;
    requires org.slf4j;
    requires java.desktop;

    exports pl.magzik.io;
    exports pl.magzik.algorithms;
    exports pl.magzik.cache;
    exports pl.magzik.predicates;
    exports pl.magzik.grouping;
    exports pl.magzik;

    opens pl.magzik;
}