import { METRAJAY_SERVER_URL, BASIC_AUTH_USERNAME, BASIC_AUTH_PASSWORD } from '@env'
import { Buffer } from "buffer";

export async function serverFetch(path:string) {
    let response = await fetch(process.env.EXPO_PUBLIC_METRAJAY_SERVER_URL + path, {
        headers: {
            Authorization: "Basic " + Buffer.from(process.env.EXPO_PUBLIC_BASIC_AUTH_USERNAME + ":" + process.env.EXPO_PUBLIC_BASIC_AUTH_PASSWORD).toString('base64')
        },
    });
    let data = await response.json();
    return data;
};
