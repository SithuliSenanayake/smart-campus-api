/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.smart.campus.api.exception;


public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException() {
        super("Sensor is currently under maintenance and cannot accept readings.");
    }
}
