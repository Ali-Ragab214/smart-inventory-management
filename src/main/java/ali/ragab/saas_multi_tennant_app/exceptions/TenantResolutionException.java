package ali.ragab.saas_multi_tennant_app.exceptions;

public class TenantResolutionException extends IllegalStateException {

    public TenantResolutionException(final String message) {
        super(message);
    }

    public TenantResolutionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
