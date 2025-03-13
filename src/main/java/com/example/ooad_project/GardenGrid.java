package com.example.ooad_project;

import com.example.ooad_project.Plant.Plant;
import com.example.ooad_project.ThreadUtils.EventBus;

import java.util.ArrayList;

public class GardenGrid {
    private static GardenGrid instance = null;
    private Plant[][] plantGrid;

    private final int numRows = 4;
    private final int numCols = 8;


    private GardenGrid() {

        plantGrid = new Plant[numRows][numCols];
        EventBus.subscribe("PlantDeathEvent", event ->  handlePlantDeath((Plant) event));
    }

    private void handlePlantDeath(Plant plant) {
        EventBus.publish("PlantDeathUIChangeEvent", plant);
        plantGrid[plant.getRow()][plant.getCol()] = null;
    }


    //    Singleton pattern
    public static GardenGrid getInstance() {
        if (instance == null) {
            instance = new GardenGrid();
        }
        return instance;
    }


    //    Just prints the grid to the console
//    If possible we need to make it better
//    For testing.
    public void printGrid() {

        System.out.println("\nGarden Grid: \n");

        for (int i = 0; i < plantGrid.length; i++) {
            for (int j = 0; j < plantGrid[i].length; j++) {
                if (plantGrid[i][j] != null) {
                    System.out.print(plantGrid[i][j].getName() + "\t");
                } else {
                    System.out.print("Empty\t");
                }
            }
            System.out.println();
        }


    }


    public synchronized ArrayList<Plant> getPlants() {
        ArrayList<Plant> plants = new ArrayList<>();
        for (int i = 0; i < plantGrid.length; i++) {
            for (int j = 0; j < plantGrid[i].length; j++) {
                if (plantGrid[i][j] != null) {
                    plants.add(plantGrid[i][j]);
                }
            }
        }
        return plants;
    }


    public synchronized void addPlant(Plant plant, int row, int col) {
        if (plantGrid[row][col] == null) {
            plantGrid[row][col] = plant;
        } else {
            throw new IllegalArgumentException("Spot at row " + row + " col " + col + " is already occupied");
        }
    }

    //    It is important that we synchronize this method
//    Because we are accessing the grid via multiple threadsg
    public synchronized Plant getPlant(int row, int col) {
        if (row >= 0 && row < getNumRows() && col >= 0 && col < getNumCols()) {
            return plantGrid[row][col];
        }
        return null;
    }

    public int getNumRows() {
        return numRows;
    }

    public int getNumCols() {
        return numCols;
    }



    public boolean isSpotOccupied(int row, int col) {
        return plantGrid[row][col] != null;
    }

}