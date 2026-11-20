package fr.citizen.fonderie.block;

/**
 * Les trois niveaux de four de forgeron.
 * w = largeur (vers la droite quand on regarde la façade), h = hauteur, d = profondeur (vers l'arrière).
 * Le nombre de blocs occupés est aussi le nombre de minerais fondus en parallèle.
 */
public enum ForgeTier {
    SIMPLE("four_forgeron_simple", 1, 2, 1, 1),
    APPRENTI("four_forgeron_apprenti", 2, 2, 1, 2),
    MAITRE("four_maitre_forgeron", 2, 2, 2, 6);

    public final String id;
    public final int w;
    public final int h;
    public final int d;
    /** Index de la partie qui porte la cheminée (fumée). */
    public final int chimneyPart;

    ForgeTier(String id, int w, int h, int d, int chimneyPart) {
        this.id = id;
        this.w = w;
        this.h = h;
        this.d = d;
        this.chimneyPart = chimneyPart;
    }

    public int blocks() {
        return w * h * d;
    }

    /** Un emplacement de minerai par bloc occupé : 2 / 4 / 8. */
    public int slots() {
        return blocks();
    }

    public int dx(int part) {
        return part % w;
    }

    public int dy(int part) {
        return (part / w) % h;
    }

    public int dz(int part) {
        return part / (w * h);
    }

    /** Partie basse en façade = bouche du four (flammes). */
    public boolean isMouth(int part) {
        return dy(part) == 0 && dz(part) == 0;
    }

    public String dimensions() {
        return w + "×" + h + "×" + d;
    }

    public static ForgeTier byOrdinal(int ordinal) {
        ForgeTier[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SIMPLE;
    }
}
