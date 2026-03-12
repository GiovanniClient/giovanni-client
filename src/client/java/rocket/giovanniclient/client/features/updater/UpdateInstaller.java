package rocket.giovanniclient.client.features.updater;

import moe.nea.libautoupdate.PotentialUpdate;

import java.util.concurrent.CompletableFuture;

public class UpdateInstaller {

    public CompletableFuture<Void> install(PotentialUpdate update) {
        return update.launchUpdate();
    }
}