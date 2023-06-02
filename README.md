# Emotion-Tracker
This is a semester project for the course **885II 22/23-MOBILE AND SOCIAL SENSING SYSTEMS [WAI-LM]** at the **University of Pisa**, A.Y 2022/23.
# Project Documentations
Just hit this [link](https://github.com/gabrielemarino-gm/Emotion-Tracker/blob/main/Documentation/MASSS-project-description-GROUP4.pdf) for the project documentation/description. Just incase you need the presentation slides, [here](https://github.com/gabrielemarino-gm/Emotion-Tracker/blob/main/Documentation/Presentation-Mobile-Project-Group4.pdf) they are!

## Part-I: Emotion detection in Deep Learning with the FER2013 dataset
-We get the dataset, FER2013, with all the 6 major emotions and the 'neutral' one included from [Kaggle](https://www.kaggle.com/datasets/msambare/fer2013). Then this dataset is preprocessed into an input 'dataset' that has two major classes named 'happy' and 'unhappy'. All the remaining  emotion classes are dropped to get a balanced dataset. Then our Binary-CNN model takes this dataset as an input to calculate the happiness level of the user in a real-time basis.
## Part-II: Geolocation of the real-time video streaming
- In the 'Emotion-tracker' the happiness of the user alongside the location where the app is utilized will be stored into a remote cloud based DB named as FirebaseDB.The user experiences are displayed on the map in UI.
## Part III: Clustering
- For a better understanding of the spatial distribution of the happiness map of the end user we used the DBSCAN clustering algorithm. This part is implemented as a service that runs in the background and this way it will not hamper the user experience in the UI.

## Part IV: The UI
- Being the crucial part of the App has a number of capabilities that it provides to the end user.
1) We are in favor of authenticated users hence there are pages to Register,login and logout.
2) Users can change their credentials when they want.
3) The user can see their happiest places on the map.
4) Users can see the Rankings of their happiest places too.
5) We tried our best to prioritize the **Accessibility** concept in our work.As a result, the app can keep the user input contents despite the orientation of the phone and also is possible to use it in different brightness levels and so much more.
7) The summary of their latest locations along with the current status of the happiness-index is also provided and other credential details of the user are depicted.
## Part VI: What the end user is kindly requested for
To use the app in its fullest capabilities the user is asked for these basic requirements.
1) Creating credentials and 'memorize' and/or 'save' them
2) Giving access to their Internet connection
3) Access to the front Camera
4) Switching on the GPS

## Finally:
Don't forget to smile !
