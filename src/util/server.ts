import { METRAJAY_SERVER_URL, BASIC_AUTH_USERNAME, BASIC_AUTH_PASSWORD } from '@env'
import { Buffer } from "buffer";

export async function serverFetch(path:string) {
    let response = await fetch(METRAJAY_SERVER_URL + path, {
        headers: {
            Authorization: "Basic " + Buffer.from(BASIC_AUTH_USERNAME + ":" + BASIC_AUTH_PASSWORD).toString('base64')
        },
    });
    let data = await response.json();
    return data;
};
