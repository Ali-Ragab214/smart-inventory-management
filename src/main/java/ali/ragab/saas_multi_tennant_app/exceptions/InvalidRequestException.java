package ali.ragab.saas_multi_tennant_app.exceptions;

public class InvalidRequestException extends BusinessException {
    public InvalidRequestException(final String message) {
        super(message);
    }
}
