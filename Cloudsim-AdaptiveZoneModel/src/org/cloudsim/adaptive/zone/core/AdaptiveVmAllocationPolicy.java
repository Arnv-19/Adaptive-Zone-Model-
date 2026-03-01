package org.cloudsim.adaptive.zone.core;

import java.util.HashMap;
import org.cloudbus.cloudsim.Log;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudsim.adaptive.zone.ml.MLVmPlacementPredictor;


/**
 * Adaptive VM Allocation Policy using the three-tier zone model
 */
public class AdaptiveVmAllocationPolicy extends VmAllocationPolicy {

    private ZoneManager zoneManager;
    private Map<String, Vm> vmTable;
    private MLVmPlacementPredictor mlPredictor;

    public AdaptiveVmAllocationPolicy(List<? extends Host> hostList, ZoneManager zoneManager) {
        super(hostList);
        this.zoneManager = zoneManager;
        this.vmTable = new HashMap<>();
        this.mlPredictor = new MLVmPlacementPredictor();
    }

    @Override
    public boolean allocateHostForVm(Vm vm) {
        // Determine which zone the VM should belong to
        int targetZone = determineVMZone(vm);

        // Assign VM to the determined zone
        zoneManager.assignVMToZone(vm, targetZone);

        // Find the best host using ML Predictor
        Host selectedHost = mlPredictor.predictBestHost(vm, getHostList());

        if (selectedHost != null && selectedHost.vmCreate(vm)) {
            vmTable.put(vm.getUid(), vm);
            mlPredictor.train(vm, selectedHost, 1.0); // Train with success score
            Log.printLine("VM " + vm.getId() + " allocated to Host " + selectedHost.getId() + " in Zone " + targetZone);
            return true;
        }

        return false;
    }

    @Override
    public boolean allocateHostForVm(Vm vm, Host host) {
        if (host.vmCreate(vm)) {
            vmTable.put(vm.getUid(), vm);

            // Determine zone and update zone manager
            int targetZone = determineVMZone(vm);
            zoneManager.assignVMToZone(vm, targetZone);

            mlPredictor.train(vm, host, 1.0); // Train with success score

            Log.printLine("VM " + vm.getId() + " allocated to specified Host " + host.getId());
            return true;
        }
        return false;
    }

    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
        // Perform zone-based optimization (dynamic resizing)
        zoneManager.performDynamicZoneResizing();
        // For each VM, check if it is in the correct zone; if not, migrate it
        for (Vm vm : vmList) {
            int correctZone = determineVMZone(vm);
            Integer currentZone = zoneManager.getVMZone(vm.getId());
            if (currentZone != null && currentZone != correctZone) {
                // Migrate VM to correct zone and a suitable host
                zoneManager.migrateVMToZoneAndHost(vm, correctZone, getHostList());
            }
        }
        return null;
    }

    @Override
    public void deallocateHostForVm(Vm vm) {
        Host host = vm.getHost();
        if (host != null) {
            host.vmDestroy(vm);
            vmTable.remove(vm.getUid());
        }
    }

    @Override
    public Host getHost(Vm vm) {
        return vm.getHost();
    }

    @Override
    public Host getHost(int vmId, int userId) {
        return vmTable.get(Vm.getUid(userId, vmId)).getHost();
    }

    /**
     * Determine which zone a VM belongs to based on resource characteristics
     */
    private int determineVMZone(Vm vm) {
        double cpu = vm.getMips();
        double ram = vm.getRam();
        double storage = vm.getSize();

        // Simple heuristic for zone assignment
        if (ram > 1500 && storage > 8000) {
            return ZoneManager.HIGH_RESOURCE_ZONE;
        } else if (cpu > 1500 && ram < 1000 && storage < 5000) {
            return ZoneManager.FAST_SMALL_ZONE;
        } else {
            return ZoneManager.MEDIUM_ZONE;
        }
    }

    /**
     * Find the best host for a VM within zone constraints
     */
    private Host findBestHostForVM(Vm vm, int targetZone) {
        Host bestHost = null;
        double bestFitScore = Double.MAX_VALUE;

        for (Host host : getHostList()) {
            if (host.isSuitableForVm(vm)) {
                double fitScore = calculateHostFitScore(host, vm, targetZone);
                if (fitScore < bestFitScore) {
                    bestFitScore = fitScore;
                    bestHost = host;
                }
            }
        }

        return bestHost;
    }

    /**
     * Calculate how well a host fits a VM for a specific zone
     */
    private double calculateHostFitScore(Host host, Vm vm, int zoneId) {
        // Consider available resources and zone characteristics
        double cpuUtilization = (double) vm.getMips() / host.getTotalMips();
        double ramUtilization = (double) vm.getRam() / host.getRam();
        double storageUtilization = (double) vm.getSize() / host.getStorage();

        // Zone-specific scoring
        switch (zoneId) {
            case ZoneManager.HIGH_RESOURCE_ZONE:
                // Prefer hosts with high available RAM and storage
                return (ramUtilization * 0.4) + (storageUtilization * 0.4) + (cpuUtilization * 0.2);

            case ZoneManager.FAST_SMALL_ZONE:
                // Prefer hosts with high available CPU
                return (cpuUtilization * 0.6) + (ramUtilization * 0.2) + (storageUtilization * 0.2);

            case ZoneManager.MEDIUM_ZONE:
            default:
                // Balanced scoring
                return (cpuUtilization + ramUtilization + storageUtilization) / 3.0;
        }
    }
}