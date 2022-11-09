export default class Paths {

    paths: any[];

	constructor(raw: any) {
        if (Object.keys(raw).length === 0) {
            this.paths = []
        } else {
            this.paths = Object.entries(raw).map(entry => {
                return {
                    ...entry[1],
                    id: entry[0]
                }
            })
        }
    }

    isEmpty() {
        return this.paths.length === 0
    }

    stationList() {
        return this.paths;
    }

    getFullName(station: string) {
        return this.paths.find(x=>x.id===station).name
    }

    // TODO: fix name, fix the empty bits, crashes page load
    stationListForStation(station: string) {
        if (this.isEmpty() || (station === '' || station === undefined)) {
            return [];
        } else {
            let routeIds = this.paths.find(x=>x.id===station).routes.map(r => r.id)

            let filtered = this.paths.filter(path => {
                let pathRoutes = path.routes.map(r => r.id)
                return routeIds.some(r => pathRoutes.includes(r))
            });
            return filtered;
        }
    }
}
