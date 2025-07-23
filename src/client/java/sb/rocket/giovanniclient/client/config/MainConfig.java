package sb.rocket.giovanniclient.client.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import sb.rocket.giovanniclient.client.features.autosolvers.AutoSolversConfig;
import sb.rocket.giovanniclient.client.features.fun.FunConfig;

public class MainConfig extends Config {
    @Override
    public String getTitle() {
        return "§bGiovanni Client";
    }

    @Expose
    @Category(name = "About", desc = "Information about GiovanniClient and updates.")
    public AboutConfig about = new AboutConfig();

    @Expose
    @Category(name = "AutoSolvers", desc = "Various auto solvers for GUIs")
    public AutoSolversConfig asc = new AutoSolversConfig();

    @Expose
    @Category(name = "Fun", desc = "becuater bagu")
    public FunConfig fc = new FunConfig();

    @Expose
    @Category(name = "Debug", desc = "here be dragons")
    public DebugConfig dc = new DebugConfig();

}