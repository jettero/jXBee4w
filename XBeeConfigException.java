
public class XBeeConfigException extends IllegalArgumentException {
    public boolean command_mode;
    public boolean user_expect;
    public boolean probably_linespeed;

    public XBeeConfigException(String error) {
        super(error);

        command_mode       = false;
        user_expect        = false;
        probably_linespeed = false;
    } 
}
