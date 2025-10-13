package ch.turic.testjar;

public class ComplexNumber {

    private final double real;
    private final double imaginary;

    public ComplexNumber(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    public double getReal() {
        return real;
    }

    public double getImaginary() {
        return imaginary;
    }

    public double getAngle() {
        return Math.atan2(imaginary, real);
    }

    public double getMagnitude() {
        return Math.sqrt(real * real + imaginary * imaginary);
    }


    public ComplexNumber add(Object other) {
        if (other instanceof ComplexNumber cn) {
            return new ComplexNumber(real + cn.real, imaginary + cn.imaginary);
        }
        if (other instanceof Double d) {
            return new ComplexNumber(real + d, imaginary);
        }
        if (other instanceof Long l) {
            return new ComplexNumber(real + l, imaginary);
        }
        throw new IllegalArgumentException("Cannot add " + other);
    }

    public ComplexNumber multiply(ComplexNumber other) {
        return new ComplexNumber(real * other.real - imaginary * other.imaginary, real * other.imaginary + imaginary * other.real);
    }

    public ComplexNumber multiply(Double other) {
        return new ComplexNumber(real * other, imaginary * other);
    }

    public ComplexNumber multiply(Long other) {
        return new ComplexNumber(real * other, imaginary * other);
    }

    public String toString() {
        return real + " + " + imaginary + "i";
    }
}
