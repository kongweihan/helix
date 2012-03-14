package com.linkedin.helix.store;

/**
 * This exception class can be used to indicate any exception during operation
 * on the propertystore
 * 
 * @author kgopalak
 * 
 */
public class PropertyStoreException extends Exception
{
  public PropertyStoreException(String msg)
  {
    super(msg);
  }

  public PropertyStoreException()
  {
    super();
  }
}