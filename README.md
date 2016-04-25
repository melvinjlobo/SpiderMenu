# SpiderMenu
A Radial Menu to showcase menu options

____
 
ScreenShot
----------
 
![Screenshot](https://github.com/melvinjlobo/SpiderMenu/blob/master/spidermenu.gif)

Usage
-----
 
Define the menu and it schildren in XML. 
```xml
<RelativeLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:paddingBottom="@dimen/activity_vertical_margin"
    android:paddingLeft="@dimen/activity_horizontal_margin"
    android:paddingRight="@dimen/activity_horizontal_margin"
    android:paddingTop="@dimen/activity_vertical_margin"
    tools:context="com.abysmel.spidermenu.MainActivity">

    <com.abysmel.spidermenu.SpiderMenu
        android:id="@+id/spidermenu"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:centerMenuRadiusSize="100dp"
        app:radialMenuRadiusSize="70dp"
        >

        <com.abysmel.spidermenu.RoundedShadowImageView
            android:id="@+id/menuitem1"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:menuType="radial"
            app:shadowRadius="3dp"
            app:shadowColor="@color/shadowColorRadial"
            app:backgroundFillColor="#FF4571AA"
            app:shadowElevation="7dp"
            app:horizontalShadowOffsetRequired="false"
            app:veticalShadowOffsetRequired="true"
            android:src="@drawable/car"/>

        <com.abysmel.spidermenu.RoundedShadowImageView
            android:id="@+id/menuitem2"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:menuType="radial"
            app:shadowRadius="3dp"
            app:shadowColor="@color/shadowColorRadial"
            app:backgroundFillColor="#FF4571AA"
            app:shadowElevation="7dp"
            app:horizontalShadowOffsetRequired="false"
            app:veticalShadowOffsetRequired="true"
            android:src="@drawable/cloud"/>

        <com.abysmel.spidermenu.RoundedShadowImageView
            android:id="@+id/menuitem3"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:menuType="radial"
            app:shadowRadius="3dp"
            app:shadowColor="@color/shadowColorRadial"
            app:backgroundFillColor="#FF4571AA"
            app:shadowElevation="7dp"
            app:horizontalShadowOffsetRequired="false"
            app:veticalShadowOffsetRequired="true"
            android:src="@drawable/mountain"/>

        <com.abysmel.spidermenu.RoundedShadowImageView
            android:id="@+id/menuitem4"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:menuType="radial"
            app:shadowRadius="3dp"
            app:shadowColor="@color/shadowColorRadial"
            app:backgroundFillColor="#FF4571AA"
            app:shadowElevation="7dp"
            app:horizontalShadowOffsetRequired="false"
            app:veticalShadowOffsetRequired="true"
            android:src="@drawable/sun"/>

        <com.abysmel.spidermenu.RoundedShadowImageView
            android:id="@+id/menuitem5"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:menuType="radial"
            app:shadowRadius="3dp"
            app:shadowColor="@color/shadowColorRadial"
            app:backgroundFillColor="#FF4571AA"
            app:shadowElevation="7dp"
            app:horizontalShadowOffsetRequired="false"
            app:veticalShadowOffsetRequired="true"
            android:src="@drawable/trees"/>

        <!-- As a thumb rule, define the center menu after the others have been defined to maintain drawing order.
             This will help the start animation where the radial pieces will be visibly coming from "below" the
             central menu item-->
        <com.abysmel.spidermenu.RoundedShadowImageView
            android:id="@+id/menuitem6"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:menuType="center"
            app:shadowRadius="4dp"
            app:shadowColor="@color/shadowColorRadial"
            app:backgroundFillColor="#FF8871AA"
            app:shadowElevation="4dp"
            app:shouldResizeBitmap="false"
            app:horizontalShadowOffsetRequired="false"
            app:veticalShadowOffsetRequired="true"
            android:src="@drawable/camera"/>

    </com.abysmel.spidermenu.SpiderMenu>

</RelativeLayout>
```

Note that the elevation works in a reverse manner. **10** being the closest to the ground (smaller shadow offset and darker shadow) and **1** being the farthest (larger shadow offset and lighter shadow). Also note that since the menu uses a bitmap shader with a CLAMP TileMode, allow at least one pixel or transparent space in your png's.

You can also use SpiderMenu#RoundedShadowImageView as an individual control!

Acknowledgement : Facebook for their awesome **[Rebound](http://facebook.github.io/rebound/)** Library

LICENSE
-------
 
The MIT License (MIT)
 
Copyright (c) 2016 Melvin Lobo
 
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
 
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
 
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

____
