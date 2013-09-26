PinchListView
=============

```PinchListView``` is a ```ListView``` implementation that will expand and collapse certain list rows when the "pinch-to-zoom" gesture is recognized on the ```ListView```.

Usage:

Add a ```PinchListView``` as an XML element to your layout, or dynamically in Java, just like a ```ListView```. 

```XML
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <me.jmhend.ui.pinchlist.PinchListView
        android:id="@+id/pinch_list"
        android:layout_height="match_parent"
        android:layout_width="match_parent" />

</RelativeLayout>

```

or...

```java
public class MainActivity extends Activity {
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        ...
        
        PinchListView pinchListView = new PinchListView(this);
        
        ...
        
    }
}
```

Your ```PinchListView```'s Adapter can be an BaseAdapter subclass. It must, however, implement ```PinchAdapter```.
```PinchAdapter``` has only one method to implement, ```isRowPinchable(int)```. It's the implementers decision
upon which rows of the ```PinchListView``` are pinchable.

Finally, in your ```BaseAdapter```'s ```getView(int, View, ViewGroup)``` method, call ```pinchListView.adjustHeight(View, int)``` on 
the ```View``` you intend to return from ```getView()```. This is required, and also means you'll need to pass a reference to
your ```PinchListView``` into your BaseAdapter.

The file ```MainActivity.java``` contains an example implementation of ```PinchAdapter```, as well as general usage of ```PinchListView```.
