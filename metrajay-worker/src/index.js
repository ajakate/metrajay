/**
 * Welcome to Cloudflare Workers! This is your first worker.
 *
 * - Run `npm run dev` in your terminal to start a development server
 * - Open a browser tab at http://localhost:8787/ to see your worker in action
 * - Run `npm run deploy` to publish your worker
 *
 * Learn more at https://developers.cloudflare.com/workers/
 */

const fetchFromMetra = async(endpoint, metraUsername, metraPassword) => {
	return await fetch(`https://gtfsapi.metrarail.com/gtfs/raw${endpoint}`, {
		headers: {
			Authorization: "Basic " + btoa(metraUsername + ":" + metraPassword),
		},
	});
}

const getLastPublished = async (metraUsername, metraPassword) => {
	const response = await fetchFromMetra("/published.txt", metraUsername, metraPassword);
	
	return new Response(response.body, {headers: {
		"Access-Control-Allow-Origin": "*",
		"Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
		"Access-Control-Allow-Headers": "*",
	}});
}

const getSchedule = async (metraUsername, metraPassword) => {
	const response = await fetchFromMetra("/schedule.zip", metraUsername, metraPassword);

	return new Response(response.body, {headers: {
		"Content-Type": "application/zip",
		"Access-Control-Allow-Origin": "*",
		"Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
		"Access-Control-Allow-Headers": "*",
	}});
}

export default {
	async fetch(request, env, ctx) {
		const metraUsername = env.METRA_API_USERNAME;
		const metraPassword = env.METRA_API_PASSWORD;

		const url = new URL(request.url);
		const searchParams = url.searchParams;
		const route = searchParams.get('route');

		switch (route) {
			case "update":
				return await getLastPublished(metraUsername, metraPassword);
			case "schedule":
				return await getSchedule(metraUsername, metraPassword);
			default:
				return new Response("Invalid route", { status: 404 });
		}
	},
};
