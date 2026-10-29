package goldmansachs.user.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Password Validator for enforcing strong password policies.
 * 
 * This component validates passwords against the following requirements:
 * - Minimum 12 characters
 * - At least one uppercase letter (A-Z)
 * - At least one lowercase letter (a-z)
 * - At least one digit (0-9)
 * - At least one special character (!@#$%^&*()-_+=[]{}|;:,.<>?)
 * 
 * These requirements ensure strong, secure passwords resistant to common attacks.
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Component
@Slf4j
public class PasswordValidator {
    
    /**
     * Minimum password length requirement.
     */
    private static final int MIN_PASSWORD_LENGTH = 12;
    
    /**
     * Regular expression pattern for uppercase letters.
     */
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    
    /**
     * Regular expression pattern for lowercase letters.
     */
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    
    /**
     * Regular expression pattern for digits.
     */
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    
    /**
     * Regular expression pattern for special characters.
     * Includes: !@#$%^&*()-_+=[]{}|;:,.<>?
     */
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()\\-_+=\\[\\]{}|;:,.<>?]");
    
    /**
     * Validate a password against the password policy requirements.
     * 
     * @param password the password to validate
     * @return true if the password meets all requirements, false otherwise
     */
    public boolean isValid(String password) {
        if (password == null) {
            log.warn("Password validation failed: password is null");
            return false;
        }
        
        return hasMinimumLength(password) &&
               hasUppercase(password) &&
               hasLowercase(password) &&
               hasDigit(password) &&
               hasSpecialCharacter(password);
    }
    
    /**
     * Validate a password and return a detailed validation result.
     * 
     * @param password the password to validate
     * @return a PasswordValidationResult containing validation status and specific failure reasons
     */
    public PasswordValidationResult validate(String password) {
        PasswordValidationResult result = new PasswordValidationResult();
        
        if (password == null) {
            result.addError("Password cannot be null");
            return result;
        }
        
        if (!hasMinimumLength(password)) {
            result.addError("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
        
        if (!hasUppercase(password)) {
            result.addError("Password must contain at least one uppercase letter (A-Z)");
        }
        
        if (!hasLowercase(password)) {
            result.addError("Password must contain at least one lowercase letter (a-z)");
        }
        
        if (!hasDigit(password)) {
            result.addError("Password must contain at least one digit (0-9)");
        }
        
        if (!hasSpecialCharacter(password)) {
            result.addError("Password must contain at least one special character (!@#$%^&*()-_+=[]{}|;:,.<>?)");
        }
        
        return result;
    }
    
    /**
     * Check if password has minimum required length.
     * 
     * @param password the password to check
     * @return true if password length is >= MIN_PASSWORD_LENGTH
     */
    private boolean hasMinimumLength(String password) {
        boolean valid = password.length() >= MIN_PASSWORD_LENGTH;
        if (!valid) {
            log.debug("Password validation failed: insufficient length. Required: {}, Actual: {}", 
                    MIN_PASSWORD_LENGTH, password.length());
        }
        return valid;
    }
    
    /**
     * Check if password contains at least one uppercase letter.
     * 
     * @param password the password to check
     * @return true if password contains uppercase letter
     */
    private boolean hasUppercase(String password) {
        boolean valid = UPPERCASE_PATTERN.matcher(password).find();
        if (!valid) {
            log.debug("Password validation failed: no uppercase letter found");
        }
        return valid;
    }
    
    /**
     * Check if password contains at least one lowercase letter.
     * 
     * @param password the password to check
     * @return true if password contains lowercase letter
     */
    private boolean hasLowercase(String password) {
        boolean valid = LOWERCASE_PATTERN.matcher(password).find();
        if (!valid) {
            log.debug("Password validation failed: no lowercase letter found");
        }
        return valid;
    }
    
    /**
     * Check if password contains at least one digit.
     * 
     * @param password the password to check
     * @return true if password contains digit
     */
    private boolean hasDigit(String password) {
        boolean valid = DIGIT_PATTERN.matcher(password).find();
        if (!valid) {
            log.debug("Password validation failed: no digit found");
        }
        return valid;
    }
    
    /**
     * Check if password contains at least one special character.
     * 
     * @param password the password to check
     * @return true if password contains special character
     */
    private boolean hasSpecialCharacter(String password) {
        boolean valid = SPECIAL_CHAR_PATTERN.matcher(password).find();
        if (!valid) {
            log.debug("Password validation failed: no special character found");
        }
        return valid;
    }
    
    /**
     * Get the minimum required password length.
     * 
     * @return the minimum password length
     */
    public int getMinPasswordLength() {
        return MIN_PASSWORD_LENGTH;
    }
    
    /**
     * Inner class representing the result of password validation.
     * Contains validation status and detailed error messages.
     */
    public static class PasswordValidationResult {
        private boolean valid = true;
        private StringBuilder errors = new StringBuilder();
        
        /**
         * Add an error message to the validation result.
         * 
         * @param error the error message to add
         */
        public void addError(String error) {
            this.valid = false;
            if (errors.length() > 0) {
                errors.append("; ");
            }
            errors.append(error);
        }
        
        /**
         * Check if the password passed validation.
         * 
         * @return true if password is valid, false otherwise
         */
        public boolean isValid() {
            return valid;
        }
        
        /**
         * Get all error messages as a single string.
         * 
         * @return comma-separated error messages
         */
        public String getErrors() {
            return errors.toString();
        }
        
        /**
         * Get error messages as an array.
         * 
         * @return array of error message strings
         */
        public String[] getErrorsAsArray() {
            if (errors.length() == 0) {
                return new String[0];
            }
            return errors.toString().split("; ");
        }
    }
}
