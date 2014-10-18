package edu.snu.bdcs.differentsc.mlpractice;

import java.util.ArrayList;

/**
 * Custom Vector class for L-BFGS
 */
public class MyVector {

  private final ArrayList<Double> contents;

  public int size() {
    return contents.size();
  }
  public double get(int index) {
    return contents.get(index);
  }
  public void add(double elem) {
    contents.add(elem);
  }
  public boolean isExist() {
    return !contents.isEmpty();
  }

  public MyVector() {
    contents = new ArrayList<Double>();
  }
  public MyVector(ArrayList<Double> list) {
    contents = new ArrayList<Double>();
    for (Double elem: list) {
      contents.add(elem);
    }
  }
  public ArrayList<Double> getArrayList() {
    return contents;
  }

  public boolean containsNaN() {

    for(double elem : contents){
      if(Double.isNaN(elem))
        return true;
    }

    return false;
  }

  public static double scalarProduct(MyVector a, MyVector b) {
    if (a.size() != b.size())
      throw new RuntimeException();
    else {
      double result = 0;
      for (int i = 0; i < a.size(); i++)
        result += a.getArrayList().get(i) * b.getArrayList().get(i);
      return result;
    }
  }
  public static MyVector simpleProduct(MyVector a, MyVector b) {
    if (a.size() != b.size())
      throw new RuntimeException();
    else {
      ArrayList<Double> result = new ArrayList<Double>();
      for (int i = 0; i < a.size(); i++) {
        result.add(a.getArrayList().get(i) * b.getArrayList().get(i));
      }
      return new MyVector(result);
    }
  }
  public static MyVector subtract(MyVector a, MyVector b) {
    if (a.size() != b.size())
      throw new RuntimeException();
    else {
      final ArrayList<Double> result = new ArrayList<Double>();
      for (int i = 0; i < a.size(); i++) {
        result.add(a.getArrayList().get(i) - b.getArrayList().get(i));
      }
      return new MyVector(result);
    }
  }
  public static MyVector add(MyVector a, MyVector b) {
    if (a.size() != b.size())
      throw new RuntimeException();
    else {
      final ArrayList<Double> result = new ArrayList<Double>();
      for (int i = 0; i < a.size(); i++) {
        result.add(a.getArrayList().get(i) + b.getArrayList().get(i));
      }
      return new MyVector(result);
    }
  }
  public static MyVector constantMultiply(double a, MyVector b) {
    final ArrayList<Double> result = new ArrayList<Double>();
    for (int i = 0; i < b.size(); i++) {
      result.add(a * b.get(i));
    }
    return new MyVector(result);
  }
}
