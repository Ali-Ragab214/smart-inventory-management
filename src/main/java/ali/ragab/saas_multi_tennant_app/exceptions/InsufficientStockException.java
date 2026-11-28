package ali.ragab.saas_multi_tennant_app.exceptions;

public class InsufficientStockException extends ConflictException {

    public InsufficientStockException(final String message) {
        super(message);
    }
}
