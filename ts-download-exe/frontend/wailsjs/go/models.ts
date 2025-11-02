export namespace main {
	
	export class ActivityConfig {
	    excelFile: string;
	    maxDays: number;
	    outputPath: string;
	
	    static createFrom(source: any = {}) {
	        return new ActivityConfig(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.excelFile = source["excelFile"];
	        this.maxDays = source["maxDays"];
	        this.outputPath = source["outputPath"];
	    }
	}
	export class ActivityResult {
	    success: boolean;
	    message: string;
	    outputPath: string;
	    rowsProcessed: number;
	
	    static createFrom(source: any = {}) {
	        return new ActivityResult(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.success = source["success"];
	        this.message = source["message"];
	        this.outputPath = source["outputPath"];
	        this.rowsProcessed = source["rowsProcessed"];
	    }
	}
	export class AgeProcessConfig {
	    excelFile: string;
	    ageColumn: string;
	    threshold: number;
	    increment: number;
	    outputPath: string;
	
	    static createFrom(source: any = {}) {
	        return new AgeProcessConfig(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.excelFile = source["excelFile"];
	        this.ageColumn = source["ageColumn"];
	        this.threshold = source["threshold"];
	        this.increment = source["increment"];
	        this.outputPath = source["outputPath"];
	    }
	}
	export class AgeProcessResult {
	    success: boolean;
	    message: string;
	    outputPath: string;
	    rowsProcessed: number;
	    rowsModified: number;
	
	    static createFrom(source: any = {}) {
	        return new AgeProcessResult(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.success = source["success"];
	        this.message = source["message"];
	        this.outputPath = source["outputPath"];
	        this.rowsProcessed = source["rowsProcessed"];
	        this.rowsModified = source["rowsModified"];
	    }
	}
	export class FilterConfig {
	    excelFile: string;
	    txtFile: string;
	    columnName: string;
	    filterType: string;
	    outputPath: string;
	
	    static createFrom(source: any = {}) {
	        return new FilterConfig(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.excelFile = source["excelFile"];
	        this.txtFile = source["txtFile"];
	        this.columnName = source["columnName"];
	        this.filterType = source["filterType"];
	        this.outputPath = source["outputPath"];
	    }
	}
	export class FilterResult {
	    success: boolean;
	    message: string;
	    outputPath: string;
	    rowsProcessed: number;
	    rowsFiltered: number;
	
	    static createFrom(source: any = {}) {
	        return new FilterResult(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.success = source["success"];
	        this.message = source["message"];
	        this.outputPath = source["outputPath"];
	        this.rowsProcessed = source["rowsProcessed"];
	        this.rowsFiltered = source["rowsFiltered"];
	    }
	}
	export class MergeConfig {
	    removeDuplicates: boolean;
	    deduplicateColumn: string;
	    outputPath: string;
	
	    static createFrom(source: any = {}) {
	        return new MergeConfig(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.removeDuplicates = source["removeDuplicates"];
	        this.deduplicateColumn = source["deduplicateColumn"];
	        this.outputPath = source["outputPath"];
	    }
	}
	export class MergeResult {
	    success: boolean;
	    message: string;
	    outputPath: string;
	    rowsProcessed: number;
	    rowsAfterDedupe: number;
	
	    static createFrom(source: any = {}) {
	        return new MergeResult(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.success = source["success"];
	        this.message = source["message"];
	        this.outputPath = source["outputPath"];
	        this.rowsProcessed = source["rowsProcessed"];
	        this.rowsAfterDedupe = source["rowsAfterDedupe"];
	    }
	}
	export class UpdateConfig {
	    mainFile: string;
	    subFile: string;
	    matchColumn: string;
	    updateColumns: string[];
	    outputPath: string;
	
	    static createFrom(source: any = {}) {
	        return new UpdateConfig(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.mainFile = source["mainFile"];
	        this.subFile = source["subFile"];
	        this.matchColumn = source["matchColumn"];
	        this.updateColumns = source["updateColumns"];
	        this.outputPath = source["outputPath"];
	    }
	}
	export class UpdateResult {
	    success: boolean;
	    message: string;
	    outputPath: string;
	    rowsProcessed: number;
	    rowsUpdated: number;
	
	    static createFrom(source: any = {}) {
	        return new UpdateResult(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.success = source["success"];
	        this.message = source["message"];
	        this.outputPath = source["outputPath"];
	        this.rowsProcessed = source["rowsProcessed"];
	        this.rowsUpdated = source["rowsUpdated"];
	    }
	}

}

