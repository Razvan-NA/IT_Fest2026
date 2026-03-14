import * as admin from "firebase-admin";
import { onDocumentCreated } from "firebase-functions/v2/firestore";

admin.initializeApp();

export const onFallDetected = onDocumentCreated(
  "fall_events/{eventId}",
  async (event) => {
    const fallData = event.data?.data();
    if (!fallData) return;

    const fromUid = fallData.uid;
    const fromName = fallData.name;

    const userDoc = await admin.firestore().collection("users").doc(fromUid).get();
    const userData = userDoc.data();
    const lat = userData?.lastLatitude ?? fallData.latitude;
    const lng = userData?.lastLongitude ?? fallData.longitude;

    const usersSnapshot = await admin.firestore().collection("users").get();
    const tokens: string[] = [];

    usersSnapshot.forEach((doc) => {
      const user = doc.data();
      if (user.uid !== fromUid && user.fcmToken) {
        tokens.push(user.fcmToken);
      }
    });

    if (tokens.length === 0) {
      console.log("No other users to notify");
      return;
    }

    const message = {
      // NO notification block — data only!
      data: {
        fromUid: fromUid,
        fromName: fromName,
        latitude: lat.toString(),
        longitude: lng.toString(),
        type: "fall_alert",
      },
      android: {
        priority: "high" as const,
      },
      tokens: tokens,
    };

    const response = await admin.messaging().sendEachForMulticast(message);
    console.log(`Sent ${response.successCount} notifications, ${response.failureCount} failed`);

    response.responses.forEach((resp, idx) => {
      if (!resp.success) {
        console.error(`Failed token ${idx}: ${resp.error?.code} - ${resp.error?.message}`);
      }
    });
  }
);