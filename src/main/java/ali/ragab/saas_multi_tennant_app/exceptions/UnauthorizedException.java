package ali.ragab.saas_multi_tennant_app.exceptions;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(final String message) {
        super(message);
    }
}