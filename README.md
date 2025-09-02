# Android Barcode Scanner

A simple, modern Android app that scans product barcodes using the device camera and displays product information from a local data source.



## Features

-   **Real-time Scanning**: Uses CameraX and Google ML Kit for fast, on-device barcode detection.
-   **Local Data Lookup**: Looks up scanned barcodes in a user-provided CSV file.
-   **Product Display**: Shows the product name and image after a successful scan.
-   **Error Handling**: Displays a placeholder if an image is not found, preventing crashes.

---

## Getting Started

To get a local copy up and running, you must provide your own data by following these steps.

### **1. Add the Product Data File**

The app reads product information from a CSV file.

-   First, create the required directory:
    ```
    app/src/main/res/raw/
    ```
-   Inside this `raw` directory, place your data file named `barcode_list.txt`.
-   The file **must** use the following CSV format:

    ```csv
    Barcode,InventoryCode,InventoryName,ImageName
    905202420242028,SKIRT-001,Asymmetrical Skirt,asymmetrical_skirt.png
    ```

### **2. Add Product Images**

-   Place all your product image files inside the following directory:
    ```
    app/src/main/res/drawable/
    ```

> **Important:** Image filenames are used as resource IDs in Android. They **must be lowercase** and use **underscores `_`** instead of spaces or hyphens (e.g., `asymmetrical_skirt.png`).

### **3. Build and Run**

Open the project in Android Studio and run it on an emulator or a physical device.

---


## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
