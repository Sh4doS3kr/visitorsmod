package com.visitors.data;

import java.util.Random;

public class ReviewTextPool {
    private static final Random RANDOM = new Random();

    private static final String[] ZERO_STARS = {
            "¡Pizzería de MIERDA! ¡Dedicáos a otra cosa, inútiles!",
            "¡Vaya puto asco de sitio! ¡Ojalá cerréis pronto! QUE OS FOLLEN HIJOS DE PUTA",
            "¡Servicio DEPLORABLE, sois una vergüenza para la hostelería! Ojala te den por culo Nokthar",
            "¡Me habéis tratado como basura! ¡Iros a tomar por saco!",
            "¡Este sitio es un INSULTO al paladar humano! ¡Qué asco dais!",
            "¡Incompetentes del carajo! ¡No sabéis ni servir un vaso de agua!",
            "¡Vaya antro de mala muerte! ¡Me dais vergüenza ajena!",
            "¡Basura de comida y basura de gente! ¡Cerrad ya el chiringuito!"
    };

    private static final String[] ONE_STAR = {
            "¡Qué asco de sitio! ¡Pésimo!",
            "¡Me muero de hambre y nadie me atiende, panda de vagos!",
            "¡Servicio LAMENTABLE! No sé cómo seguís abiertos.",
            "¡No vuelvo ni aunque me paguen!",
            "¡Sucio, lento y la comida es una bazofia!",
            "¡Me han tratado fatal! No tenéis ni idea de educación.",
            "¡Lo peor que he visto en años! Un desastre total."
    };

    private static final String[] TWO_STARS = {
            "Podría ser mejor, pero deja mucho que desear.",
            "Muy lento, me han crecido canas esperando.",
            "La comida estaba fría y el trato fue mediocre.",
            "No me ha gustado mucho, falta profesionalidad.",
            "Regular tirando a mal. Una decepción.",
            "Sitio aburrido y servicio distraído."
    };

    private static final String[] THREE_STARS = {
            "Ni fu ni fa, para un apuro vale.",
            "Aceptable, pero nada del otro mundo.",
            "Comida normalita, nada que destacar.",
            "Un poco ruidoso y gente amontonada.",
            "Pasable, aunque le falta chispa.",
            "Sitio decente para pasar el rato."
    };

    private static final String[] FOUR_STARS = {
            "¡Muy bueno! Se nota el esfuerzo.",
            "Me ha gustado bastante, volveré.",
            "Buen servicio y ambiente agradable.",
            "La pizza estaba rica, de las mejores por aquí.",
            "Volveré pronto, me habéis ganado.",
            "Calidad-precio muy equilibrada."
    };

    private static final String[] FIVE_STARS = {
            "¡INCREÍBLE! ¡Experiencia de 10!",
            "¡La mejor pizzería del mundo, sin duda!",
            "¡Excelencia pura en el trato y la comida!",
            "¡Delicioso, rápido y con un trato exquisito!",
            "¡Recomendadísimo! El mejor sitio de la zona.",
            "¡Simplemente perfecto! ¡Así se hacen las cosas!"
    };

    public static String getReview(int stars) {
        if (stars <= 0)
            return ZERO_STARS[RANDOM.nextInt(ZERO_STARS.length)];
        if (stars == 1)
            return ONE_STAR[RANDOM.nextInt(ONE_STAR.length)];
        if (stars == 2)
            return TWO_STARS[RANDOM.nextInt(TWO_STARS.length)];
        if (stars == 3)
            return THREE_STARS[RANDOM.nextInt(THREE_STARS.length)];
        if (stars == 4)
            return FOUR_STARS[RANDOM.nextInt(FOUR_STARS.length)];
        return FIVE_STARS[RANDOM.nextInt(FIVE_STARS.length)];
    }
}
