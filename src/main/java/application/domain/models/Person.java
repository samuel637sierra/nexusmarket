package application.domain.models;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Información común de las personas que participan en NexusMarket.
 */
public class Person {

    private String personId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private LocalDate birthDate;

    public Person() {
    }

    public Person(String personId, String fullName, String email, String phoneNumber,
                  String address, LocalDate birthDate) {
        setPersonId(personId);
        setFullName(fullName);
        setEmail(email);
        setPhoneNumber(phoneNumber);
        setAddress(address);
        setBirthDate(birthDate);
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = requireText(personId, "El identificador de la persona es obligatorio");
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = requireText(fullName, "El nombre completo es obligatorio");
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = requireText(email, "El correo electrónico es obligatorio");
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = requireText(phoneNumber, "El número de teléfono es obligatorio");
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = requireText(address, "La dirección es obligatoria");
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
        this.birthDate = birthDate;
    }

    /** Alias compatible con la nomenclatura original del documento. */
    public String getIdentification() {
        return personId;
    }

    public void setIdentification(String identification) {
        setPersonId(identification);
    }

    /** Alias compatible con la nomenclatura original del documento. */
    public String getName() {
        return fullName;
    }

    public void setName(String name) {
        setFullName(name);
    }

    public void updateContactInfo(String email, String phoneNumber) {
        setEmail(email);
        setPhoneNumber(phoneNumber);
    }

    public void updateAddress(String address) {
        setAddress(address);
    }

    public int calculateAge() {
        return calculateAge(LocalDate.now());
    }

    public int calculateAge(LocalDate referenceDate) {
        if (birthDate == null) {
            throw new IllegalStateException("La fecha de nacimiento es obligatoria");
        }
        Objects.requireNonNull(referenceDate, "La fecha de referencia es obligatoria");
        if (birthDate.isAfter(referenceDate)) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
        return Period.between(birthDate, referenceDate).getYears();
    }

    public String getFullInformation() {
        return "Persona{identificador='" + personId + "', nombre='" + fullName + "'}";
    }

    @Override
    public String toString() {
        return getFullInformation();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
