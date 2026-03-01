package org.cloudsim.adaptive.zone.ml;

import java.util.ArrayList;
import java.util.List;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;

/**
 * Machine Learning-based VM Placement Predictor
 * Uses K-Nearest Neighbors (KNN) algorithm for placement decisions
 */
public class MLVmPlacementPredictor {

    private List<PlacementRecord> trainingData;
    private int k = 5; // Number of neighbors for KNN
    
    public MLVmPlacementPredictor() {
        this.trainingData = new ArrayList<>();
    }
    
    /**
     * Train the model with historical placement data
     */
    public void train(Vm vm, Host host, double performanceScore) {
        PlacementRecord record = new PlacementRecord(
            vm.getMips(),
            vm.getRam(),
            vm.getSize(),
            host.getTotalMips(),
            host.getRam(),
            host.getStorage(),
            calculateHostUtilization(host),
            performanceScore
        );
        trainingData.add(record);
        
        // Keep training data manageable (max 1000 records)
        if (trainingData.size() > 1000) {
            trainingData.remove(0);
        }
    }
    
    /**
     * Predict best host for VM using KNN algorithm
     */
    public Host predictBestHost(Vm vm, List<? extends Host> hostList) {
        if (trainingData.isEmpty() || hostList.isEmpty()) {
            return findHostByHeuristic(vm, hostList);
        }
        
        Host bestHost = null;
        double bestScore = Double.MIN_VALUE;
        
        for (Host host : hostList) {
            if (!host.isSuitableForVm(vm)) {
                continue;
            }
            
            double predictedScore = predictPlacementScore(vm, host);
            if (predictedScore > bestScore) {
                bestScore = predictedScore;
                bestHost = host;
            }
        }
        
        return bestHost != null ? bestHost : findHostByHeuristic(vm, hostList);
    }
    
    /**
     * Predict placement score using KNN
     */
    private double predictPlacementScore(Vm vm, Host host) {
        List<DistanceRecord> distances = new ArrayList<>();
        
        // Calculate normalized features
        double vmCpuNorm = normalizeValue(vm.getMips(), 0, 5000);
        double vmRamNorm = normalizeValue(vm.getRam(), 0, 8192);
        double vmStorageNorm = normalizeValue(vm.getSize(), 0, 50000);
        double hostCpuNorm = normalizeValue(host.getTotalMips(), 0, 10000);
        double hostRamNorm = normalizeValue(host.getRam(), 0, 16384);
        double hostStorageNorm = normalizeValue(host.getStorage(), 0, 100000);
        double hostUtilNorm = calculateHostUtilization(host);
        
        // Find k nearest neighbors
        for (PlacementRecord record : trainingData) {
            double distance = calculateEuclideanDistance(
                vmCpuNorm, vmRamNorm, vmStorageNorm,
                hostCpuNorm, hostRamNorm, hostStorageNorm, hostUtilNorm,
                record
            );
            distances.add(new DistanceRecord(distance, record.performanceScore));
        }
        
        // Sort by distance and take k nearest
        distances.sort((a, b) -> Double.compare(a.distance, b.distance));
        
        // Calculate weighted average of k nearest neighbors
        double totalWeight = 0.0;
        double weightedSum = 0.0;
        int neighbors = Math.min(k, distances.size());
        
        for (int i = 0; i < neighbors; i++) {
            double weight = 1.0 / (distances.get(i).distance + 0.001); // Add small value to avoid division by zero
            weightedSum += weight * distances.get(i).score;
            totalWeight += weight;
        }
        
        return totalWeight > 0 ? weightedSum / totalWeight : 0.5;
    }
    
    /**
     * Calculate Euclidean distance between current VM-Host pair and training record
     */
    private double calculateEuclideanDistance(
            double vmCpu, double vmRam, double vmStorage,
            double hostCpu, double hostRam, double hostStorage, double hostUtil,
            PlacementRecord record) {
        
        double vmCpuNorm = normalizeValue(record.vmMips, 0, 5000);
        double vmRamNorm = normalizeValue(record.vmRam, 0, 8192);
        double vmStorageNorm = normalizeValue(record.vmStorage, 0, 50000);
        double hostCpuNorm = normalizeValue(record.hostMips, 0, 10000);
        double hostRamNorm = normalizeValue(record.hostRam, 0, 16384);
        double hostStorageNorm = normalizeValue(record.hostStorage, 0, 100000);
        
        double sum = Math.pow(vmCpu - vmCpuNorm, 2) +
                     Math.pow(vmRam - vmRamNorm, 2) +
                     Math.pow(vmStorage - vmStorageNorm, 2) +
                     Math.pow(hostCpu - hostCpuNorm, 2) +
                     Math.pow(hostRam - hostRamNorm, 2) +
                     Math.pow(hostStorage - hostStorageNorm, 2) +
                     Math.pow(hostUtil - record.hostUtilization, 2);
        
        return Math.sqrt(sum);
    }
    
    /**
     * Normalize value to [0, 1] range
     */
    private double normalizeValue(double value, double min, double max) {
        if (max - min == 0) return 0.5;
        return (value - min) / (max - min);
    }
    
    /**
     * Calculate host utilization
     */
    private double calculateHostUtilization(Host host) {
        double usedMips = 0;
        for (Vm vm : host.getVmList()) {
            usedMips += vm.getMips();
        }
        double cpuUtil = host.getTotalMips() > 0 ? usedMips / host.getTotalMips() : 0.0;
        double ramUtil = host.getRam() > 0 ? (double) host.getRamProvisioner().getUsedRam() / host.getRam() : 0.0;
        double storageUtil = 0.0; // CloudSim 3.0.3 standard Host does not track dynamic storage use natively
        
        return (cpuUtil * 0.4 + ramUtil * 0.3 + storageUtil * 0.3);
    }
    
    /**
     * Fallback heuristic when ML model has no training data
     */
    private Host findHostByHeuristic(Vm vm, List<? extends Host> hostList) {
        Host bestHost = null;
        double bestScore = Double.MAX_VALUE;
        
        for (Host host : hostList) {
            if (host.isSuitableForVm(vm)) {
                double utilization = calculateHostUtilization(host);
                double fitScore = Math.abs(utilization - 0.6); // Target 60% utilization
                
                if (fitScore < bestScore) {
                    bestScore = fitScore;
                    bestHost = host;
                }
            }
        }
        
        return bestHost;
    }
    
    /**
     * Get model statistics
     */
    public int getTrainingDataSize() {
        return trainingData.size();
    }
    
    /**
     * Clear training data
     */
    public void clearTrainingData() {
        trainingData.clear();
    }
    
    /**
     * Inner class for training records
     */
    private static class PlacementRecord {
        double vmMips, vmRam, vmStorage;
        double hostMips, hostRam, hostStorage;
        double hostUtilization;
        double performanceScore;
        
        PlacementRecord(double vmMips, double vmRam, double vmStorage,
                       double hostMips, double hostRam, double hostStorage,
                       double hostUtil, double score) {
            this.vmMips = vmMips;
            this.vmRam = vmRam;
            this.vmStorage = vmStorage;
            this.hostMips = hostMips;
            this.hostRam = hostRam;
            this.hostStorage = hostStorage;
            this.hostUtilization = hostUtil;
            this.performanceScore = score;
        }
    }
    
    /**
     * Inner class for distance calculations
     */
    private static class DistanceRecord {
        double distance;
        double score;
        
        DistanceRecord(double distance, double score) {
            this.distance = distance;
            this.score = score;
        }
    }
}