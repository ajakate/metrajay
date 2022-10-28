export default class Paths {

	constructor(
		public paths: any,
	) { }

    isEmpty() {
        return Object.keys(this.paths).length === 0
    }

    // TODO: deletemedebug
    getName(id: string) {
        if (this.isEmpty()) {
            return "nope"
        } else {
            return this.paths[id]['name']
        }
    }
}
