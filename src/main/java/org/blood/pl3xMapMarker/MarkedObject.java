package org.blood.pl3xMapMarker;

public final class MarkedObject {

    public enum Type {
        MARKER,
        TERRITORY
    }

    private final int id;
    private final Type type;
    private final String world;
    private final String label;
    private final String author;
    private final int color; // ARGB, only for territories
    private final double x1;
    private final double z1;
    private final double x2; // only for territories
    private final double z2; // only for territories

    public MarkedObject(int id, Type type, String world, String label, String author,
                        int color, double x1, double z1, double x2, double z2) {
        this.id = id;
        this.type = type;
        this.world = world;
        this.label = label;
        this.author = author;
        this.color = color;
        this.x1 = x1;
        this.z1 = z1;
        this.x2 = x2;
        this.z2 = z2;
    }

    public int getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public String getWorld() {
        return world;
    }

    public String getLabel() {
        return label;
    }

    public String getAuthor() {
        return author;
    }

    public int getColor() {
        return color;
    }

    public double getX1() {
        return x1;
    }

    public double getZ1() {
        return z1;
    }

    public double getX2() {
        return x2;
    }

    public double getZ2() {
        return z2;
    }

    public String getMarkerKey() {
        return "mapmark_" + id;
    }
}
