package br.com.chatbot.chatbot_api.entity;

import java.io.Serializable;
import java.util.Arrays;

public class PGvector implements Serializable {

    private final float[] vector;

    public PGvector(float[] vector) {
        this.vector = vector;
    }

    public PGvector(double[] vector) {
        this.vector = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            this.vector[i] = (float) vector[i];
        }
    }

    public float[] getVector() {
        return vector;
    }

    @Override
    public String toString() {
        return Arrays.toString(vector);
    }

    private static final long serialVersionUID = 1L;
}
