Android Barcode Scanner
A simple app that scans product barcodes and displays their name and image from a local data file.

##Setup
To run this project, you must provide your own data:

###Add Data File: 

Create the directory app/src/main/res/raw and place your barcode_list.txt file inside. The file must be a CSV with this exact format:

Barcode,InventoryCode,InventoryName,ImageName
905202420242028,SKIRT-001,Asymmetrical Skirt,asymmetrical_skirt.png


###Add Image Files: Place all product images in the app/src/main/res/drawable/ folder.

Important: Image names must be lowercase and use underscores instead of spaces or hyphens (e.g., asymmetrical_skirt.png).

Build and Run the project in Android Studio.