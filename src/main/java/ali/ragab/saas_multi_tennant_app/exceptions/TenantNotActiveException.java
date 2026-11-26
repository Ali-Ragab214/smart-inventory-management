package ali.ragab.saas_multi_tennant_app.exceptions;

public class TenantNotActiveException extends TenantResolutionException {

    public TenantNotActiveException(final String message) {
        super(message);
    }
}
