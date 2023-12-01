package group.aelysium.rustyconnector.plugin.velocity.lib.load_balancing;

import group.aelysium.rustyconnector.core.lib.algorithm.QuickSort;
import group.aelysium.rustyconnector.core.lib.algorithm.SingleSort;
import group.aelysium.rustyconnector.core.lib.algorithm.WeightedQuickSort;
import group.aelysium.rustyconnector.plugin.velocity.lib.server.MCLoader;

public class LeastConnection extends LoadBalancer {

    public LeastConnection(Settings settings) {
        super(settings);
    }

    @Override
    public void iterate() {
        try {
            MCLoader thisItem = this.servers.get(this.index);
            MCLoader theNextItem = this.servers.get(this.index + 1);

            if(thisItem.playerCount() >= theNextItem.playerCount()) this.index++;
        } catch (IndexOutOfBoundsException ignore) {}
    }

    @Override
    public void completeSort() {
        this.index = 0;
        if(this.weighted()) WeightedQuickSort.sort(this.servers);
        else QuickSort.sort(this.servers);
    }

    @Override
    public void singleSort() {
        this.index = 0;
        SingleSort.sort(this.servers, this.index);
    }

    @Override
    public String toString() {
        return "LoadBalancer (LeastConnection): "+this.size()+" items";
    }
}
