package com.home.ma.photolocationnote.azure;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

public class ImageManager {

    public static final String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=photolocation;"+
            "AccountKey=DbafKFK4nzlOZRpEMo7UlOuPhEAsPBuY+Go51pk0LCC1nHYqlT9ZLK9lsemlCZY8L+MwpWahv09l3N8wJs+NLA==;"+
            "EndpointSuffix=core.windows.net";

    private static CloudBlobContainer getContainer(){
        // Retrieve storage account from connection-string.
        try {
            CloudStorageAccount storageAccount = CloudStorageAccount
                    .parse(storageConnectionString);
            // Create the blob client.
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            // Get a reference to a container.
            // The container name must be lower case
            CloudBlobContainer container = blobClient.getContainerReference("images");
            return container;
        }catch (Exception ex){
            System.out.printf(ex.getMessage());
        }
        return null;
    }

    public static String UploadImage(InputStream image, int imageLength, String imageName ) throws Exception {
        CloudBlobContainer container = getContainer();
        container.createIfNotExists();
        CloudBlockBlob imageBlob = container.getBlockBlobReference(imageName);
        imageBlob.upload(image, imageLength);
        return imageName;
    }

    public static String[] ListImages() throws Exception{
        CloudBlobContainer container = getContainer();

        Iterable<ListBlobItem> blobs = container.listBlobs();

        LinkedList<String> blobNames = new LinkedList<>();
        for(ListBlobItem blob: blobs) {
            blobNames.add(((CloudBlockBlob) blob).getName());
        }

        return blobNames.toArray(new String[blobNames.size()]);
    }

    public static void GetImage(String name, OutputStream imageStream, long imageLength) throws Exception {
        CloudBlobContainer container = getContainer();

        CloudBlockBlob blob = container.getBlockBlobReference(name);

        if(blob.exists()){
            blob.downloadAttributes();

            imageLength = blob.getProperties().getLength();

            blob.download(imageStream);
        }
    }

}
