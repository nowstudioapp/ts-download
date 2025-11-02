package main

import (
	"bufio"
	"context"
	"encoding/csv"
	"fmt"
	"math/rand"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/wailsapp/wails/v2/pkg/runtime"
	"github.com/xuri/excelize/v2"
)

// App struct
type App struct {
	ctx context.Context
}

// NewApp creates a new App application struct
func NewApp() *App {
	return &App{}
}

// startup is called when the app starts. The context is saved
// so we can call the runtime methods
func (a *App) startup(ctx context.Context) {
	a.ctx = ctx
}

// MergeConfig 合并配置
type MergeConfig struct {
	RemoveDuplicates  bool   `json:"removeDuplicates"`
	DeduplicateColumn string `json:"deduplicateColumn"`
	OutputPath        string `json:"outputPath"`
}

// MergeResult 合并结果
type MergeResult struct {
	Success         bool   `json:"success"`
	Message         string `json:"message"`
	OutputPath      string `json:"outputPath"`
	RowsProcessed   int    `json:"rowsProcessed"`
	RowsAfterDedupe int    `json:"rowsAfterDedupe"`
}

// FilterConfig 过滤配置
type FilterConfig struct {
	ExcelFile  string `json:"excelFile"`
	TxtFile    string `json:"txtFile"`
	ColumnName string `json:"columnName"`
	FilterType string `json:"filterType"` // "include" 或 "exclude"
	OutputPath string `json:"outputPath"`
}

// FilterResult 过滤结果
type FilterResult struct {
	Success       bool   `json:"success"`
	Message       string `json:"message"`
	OutputPath    string `json:"outputPath"`
	RowsProcessed int    `json:"rowsProcessed"`
	RowsFiltered  int    `json:"rowsFiltered"`
}

// UpdateConfig 更新配置
type UpdateConfig struct {
	MainFile      string   `json:"mainFile"`
	SubFile       string   `json:"subFile"`
	MatchColumn   string   `json:"matchColumn"`
	UpdateColumns []string `json:"updateColumns"`
	OutputPath    string   `json:"outputPath"`
}

// UpdateResult 更新结果
type UpdateResult struct {
	Success       bool   `json:"success"`
	Message       string `json:"message"`
	OutputPath    string `json:"outputPath"`
	RowsProcessed int    `json:"rowsProcessed"`
	RowsUpdated   int    `json:"rowsUpdated"`
}

// AgeProcessConfig 年龄处理配置
type AgeProcessConfig struct {
	ExcelFile  string `json:"excelFile"`
	AgeColumn  string `json:"ageColumn"`
	Threshold  int    `json:"threshold"`
	Increment  int    `json:"increment"`
	OutputPath string `json:"outputPath"`
}

// AgeProcessResult 年龄处理结果
type AgeProcessResult struct {
	Success       bool   `json:"success"`
	Message       string `json:"message"`
	OutputPath    string `json:"outputPath"`
	RowsProcessed int    `json:"rowsProcessed"`
	RowsModified  int    `json:"rowsModified"`
}

// ActivityConfig 活跃数据生成配置
type ActivityConfig struct {
	ExcelFile  string `json:"excelFile"`
	MaxDays    int    `json:"maxDays"`
	OutputPath string `json:"outputPath"`
}

// ActivityResult 活跃数据生成结果
type ActivityResult struct {
	Success       bool   `json:"success"`
	Message       string `json:"message"`
	OutputPath    string `json:"outputPath"`
	RowsProcessed int    `json:"rowsProcessed"`
}

// MergeExcelFiles 合并 Excel 文件
func (a *App) MergeExcelFiles(filePaths []string, config MergeConfig) MergeResult {
	if len(filePaths) == 0 {
		return MergeResult{
			Success: false,
			Message: "未选择任何文件",
		}
	}

	// 读取所有文件数据
	var allRows []map[string]interface{}
	var headers []string
	totalRows := 0

	for i, filePath := range filePaths {
		fmt.Printf("正在读取: %s\n", filepath.Base(filePath))
		fmt.Printf("原始路径: %s\n", filePath)

		// 如果只是文件名，尝试在常见目录查找
		actualPath := filePath
		if !filepath.IsAbs(filePath) {
			// 尝试在用户下载目录查找
			userHome, _ := os.UserHomeDir()
			downloadPath := filepath.Join(userHome, "Downloads", filePath)
			if _, err := os.Stat(downloadPath); err == nil {
				actualPath = downloadPath
				fmt.Printf("在下载目录找到文件: %s\n", actualPath)
			} else {
				// 尝试在当前目录查找
				currentDir, _ := os.Getwd()
				currentPath := filepath.Join(currentDir, filePath)
				if _, err := os.Stat(currentPath); err == nil {
					actualPath = currentPath
					fmt.Printf("在当前目录找到文件: %s\n", actualPath)
				} else {
					// 尝试在桌面查找
					desktopPath := filepath.Join(userHome, "Desktop", filePath)
					if _, err := os.Stat(desktopPath); err == nil {
						actualPath = desktopPath
						fmt.Printf("在桌面找到文件: %s\n", actualPath)
					}
				}
			}
		}

		// 检查文件是否存在
		if _, err := os.Stat(actualPath); os.IsNotExist(err) {
			return MergeResult{
				Success: false,
				Message: fmt.Sprintf("文件不存在: %s (已搜索: Downloads, Desktop, 当前目录)", filepath.Base(filePath)),
			}
		}

		filePath = actualPath // 使用找到的完整路径

		fmt.Printf("开始打开文件...\n")

		var rows [][]string
		var err error

		// 根据文件类型选择读取方式
		if isCSVFile(filePath) {
			fmt.Printf("检测到CSV文件，使用CSV读取器\n")
			rows, err = readCSVFile(filePath)
			if err != nil {
				fmt.Printf("读取CSV文件失败: %v\n", err)
				return MergeResult{
					Success: false,
					Message: fmt.Sprintf("读取CSV文件失败: %s - %v", filepath.Base(filePath), err),
				}
			}
			fmt.Printf("CSV文件读取成功，行数: %d\n", len(rows))
		} else if isExcelFile(filePath) {
			fmt.Printf("检测到Excel文件，使用Excel读取器\n")
			f, err := excelize.OpenFile(filePath)
			if err != nil {
				fmt.Printf("打开Excel文件失败: %v\n", err)
				return MergeResult{
					Success: false,
					Message: fmt.Sprintf("打开Excel文件失败: %s - %v", filepath.Base(filePath), err),
				}
			}
			defer f.Close()

			// 获取第一个工作表
			sheets := f.GetSheetList()
			if len(sheets) == 0 {
				return MergeResult{
					Success: false,
					Message: fmt.Sprintf("Excel文件为空: %s", filepath.Base(filePath)),
				}
			}

			fmt.Printf("开始读取工作表: %s\n", sheets[0])

			rows, err = f.GetRows(sheets[0])
			if err != nil {
				fmt.Printf("读取工作表失败: %v\n", err)
				return MergeResult{
					Success: false,
					Message: fmt.Sprintf("读取工作表失败: %s - %v", filepath.Base(filePath), err),
				}
			}
			fmt.Printf("Excel工作表读取成功，行数: %d\n", len(rows))
		} else {
			return MergeResult{
				Success: false,
				Message: fmt.Sprintf("不支持的文件格式: %s (仅支持 .xlsx, .xls, .csv)", filepath.Base(filePath)),
			}
		}

		// 显示文件行数信息
		fmt.Printf("文件总行数: %d 行\n", len(rows))

		if len(rows) == 0 {
			continue
		}

		// 第一个文件的第一行作为表头
		if i == 0 {
			headers = rows[0]
			fmt.Printf("设置表头: %v\n", headers)
		} else {
			// 检查其他文件的表头是否匹配
			currentHeaders := rows[0]
			fmt.Printf("当前文件表头: %v\n", currentHeaders)
			if len(currentHeaders) != len(headers) {
				fmt.Printf("警告: 表头列数不匹配! 期望 %d 列，实际 %d 列\n", len(headers), len(currentHeaders))
			}
		}

		// 转换为 map 格式
		fmt.Printf("开始转换数据格式...\n")
		totalDataRows := len(rows) - 1
		for j := 1; j < len(rows); j++ {
			// 根据文件大小调整进度显示频率
			var progressInterval int
			if totalDataRows > 100000 {
				progressInterval = 10000 // 大文件每10000行显示一次
			} else if totalDataRows > 10000 {
				progressInterval = 5000 // 中等文件每5000行显示一次
			} else {
				progressInterval = 1000 // 小文件每1000行显示一次
			}

			if j%progressInterval == 0 {
				fmt.Printf("已处理 %d/%d 行 (%.1f%%)\n", j, totalDataRows, float64(j)/float64(totalDataRows)*100)
			}

			row := make(map[string]interface{})
			for k, header := range headers {
				if k < len(rows[j]) {
					row[header] = rows[j][k]
				} else {
					row[header] = ""
				}
			}
			allRows = append(allRows, row)

			// 调试：显示前几行数据
			if j <= 3 {
				fmt.Printf("第%d行数据: %v\n", j, row)
			}
		}
		fmt.Printf("文件 %s 处理完成\n", filepath.Base(filePath))

		totalRows += len(rows) - 1
	}

	if len(allRows) == 0 {
		return MergeResult{
			Success: false,
			Message: "没有读取到任何数据",
		}
	}

	// 去重处理
	rowsAfterDedupe := len(allRows)
	fmt.Printf("合并前总行数: %d\n", len(allRows))
	if config.RemoveDuplicates && config.DeduplicateColumn != "" {
		fmt.Printf("开始去重，去重列: %s\n", config.DeduplicateColumn)
		allRows = removeDuplicates(allRows, config.DeduplicateColumn)
		rowsAfterDedupe = len(allRows)
		fmt.Printf("去重后行数: %d\n", rowsAfterDedupe)
	}

	// 创建新的 Excel 文件
	newFile := excelize.NewFile()
	defer newFile.Close()

	// 写入表头
	for i, header := range headers {
		cell := getCellName(i+1, 1)
		newFile.SetCellValue("Sheet1", cell, header)
	}

	// 写入数据
	fmt.Printf("开始写入 %d 行数据到Excel文件...\n", len(allRows))
	for i, row := range allRows {
		for j, header := range headers {
			cell := getCellName(j+1, i+2)
			newFile.SetCellValue("Sheet1", cell, row[header])
		}
		// 显示写入进度
		if i > 0 && i%10000 == 0 {
			fmt.Printf("已写入 %d/%d 行\n", i, len(allRows))
		}
	}
	fmt.Printf("数据写入完成，总共写入 %d 行\n", len(allRows))

	// 保存文件
	if err := newFile.SaveAs(config.OutputPath); err != nil {
		return MergeResult{
			Success: false,
			Message: fmt.Sprintf("保存文件失败: %v", err),
		}
	}

	return MergeResult{
		Success:         true,
		Message:         "合并完成！",
		OutputPath:      config.OutputPath,
		RowsProcessed:   totalRows,
		RowsAfterDedupe: rowsAfterDedupe,
	}
}

// getCellName 将行列坐标转换为单元格名称 (如 A1, B2, etc)
func getCellName(col, row int) string {
	// 列号转字母
	colName := ""
	for col > 0 {
		col--
		colName = string(rune('A'+col%26)) + colName
		col /= 26
	}
	return fmt.Sprintf("%s%d", colName, row)
}

// removeDuplicates 按指定列去重
func removeDuplicates(rows []map[string]interface{}, column string) []map[string]interface{} {
	seen := make(map[string]bool)
	var result []map[string]interface{}

	for _, row := range rows {
		key := fmt.Sprintf("%v", row[column])
		if !seen[key] {
			seen[key] = true
			result = append(result, row)
		}
	}

	return result
}

// SelectSaveFile 选择保存文件位置
func (a *App) SelectSaveFile() (string, error) {
	selection, err := runtime.SaveFileDialog(a.ctx, runtime.SaveDialogOptions{
		DefaultDirectory: "",
		DefaultFilename:  "merged.xlsx",
		Title:            "选择输出文件位置",
		Filters: []runtime.FileFilter{
			{
				DisplayName: "Excel Files",
				Pattern:     "*.xlsx",
			},
		},
		ShowHiddenFiles:      false,
		CanCreateDirectories: true,
	})

	return selection, err
}

// SelectExcelFiles 选择多个 Excel 文件
func (a *App) SelectExcelFiles() ([]string, error) {
	selections, err := runtime.OpenMultipleFilesDialog(a.ctx, runtime.OpenDialogOptions{
		Title: "选择要合并的 Excel 文件",
		Filters: []runtime.FileFilter{
			{
				DisplayName: "Excel Files",
				Pattern:     "*.xlsx;*.xls",
			},
		},
	})

	return selections, err
}

// SelectTxtFile 选择单个 TXT 文件
func (a *App) SelectTxtFile() (string, error) {
	selection, err := runtime.OpenFileDialog(a.ctx, runtime.OpenDialogOptions{
		Title: "选择 TXT 文件",
		Filters: []runtime.FileFilter{
			{
				DisplayName: "Text Files",
				Pattern:     "*.txt",
			},
		},
	})

	return selection, err
}

// Greet 测试函数
func (a *App) Greet(name string) string {
	return fmt.Sprintf("Hello %s!", name)
}

// GetExcelFiles 获取文件夹中的 Excel 文件
func (a *App) GetExcelFiles(folderPath string) ([]string, error) {
	var excelFiles []string

	entries, err := os.ReadDir(folderPath)
	if err != nil {
		return nil, err
	}

	for _, entry := range entries {
		if !entry.IsDir() {
			name := entry.Name()
			if strings.HasSuffix(strings.ToLower(name), ".xlsx") || strings.HasSuffix(strings.ToLower(name), ".xls") {
				excelFiles = append(excelFiles, filepath.Join(folderPath, name))
			}
		}
	}

	return excelFiles, nil
}

// FilterExcelFile 根据TXT文件过滤Excel文件
func (a *App) FilterExcelFile(config FilterConfig) FilterResult {
	// 验证输入参数
	if config.ExcelFile == "" || config.TxtFile == "" || config.OutputPath == "" || config.ColumnName == "" {
		return FilterResult{
			Success: false,
			Message: "请完善所有必填项",
		}
	}

	// 处理Excel文件路径
	excelPath := findActualPath(config.ExcelFile)
	if excelPath == "" {
		return FilterResult{
			Success: false,
			Message: fmt.Sprintf("找不到Excel文件: %s", config.ExcelFile),
		}
	}

	// 处理TXT文件路径
	txtPath := findActualPath(config.TxtFile)
	if txtPath == "" {
		return FilterResult{
			Success: false,
			Message: fmt.Sprintf("找不到TXT文件: %s", config.TxtFile),
		}
	}

	// 读取TXT文件内容
	txtValues, err := readTxtFile(txtPath)
	if err != nil {
		return FilterResult{
			Success: false,
			Message: fmt.Sprintf("读取TXT文件失败: %v", err),
		}
	}

	if len(txtValues) == 0 {
		return FilterResult{
			Success: false,
			Message: "TXT文件为空",
		}
	}

	// 将TXT内容转换为map以便快速查找
	txtMap := make(map[string]bool)
	for _, value := range txtValues {
		txtMap[strings.TrimSpace(value)] = true
	}

	// 打开Excel文件
	f, err := excelize.OpenFile(excelPath)
	if err != nil {
		return FilterResult{
			Success: false,
			Message: fmt.Sprintf("打开Excel文件失败: %v", err),
		}
	}
	defer f.Close()

	// 获取第一个工作表
	sheets := f.GetSheetList()
	if len(sheets) == 0 {
		return FilterResult{
			Success: false,
			Message: "Excel文件为空",
		}
	}

	// 读取所有行
	rows, err := f.GetRows(sheets[0])
	if err != nil {
		return FilterResult{
			Success: false,
			Message: fmt.Sprintf("读取Excel工作表失败: %v", err),
		}
	}

	if len(rows) == 0 {
		return FilterResult{
			Success: false,
			Message: "Excel文件没有数据",
		}
	}

	// 找到目标列的索引
	headers := rows[0]
	columnIndex := -1
	for i, header := range headers {
		if strings.TrimSpace(header) == strings.TrimSpace(config.ColumnName) {
			columnIndex = i
			break
		}
	}

	if columnIndex == -1 {
		return FilterResult{
			Success: false,
			Message: fmt.Sprintf("未找到列名: %s", config.ColumnName),
		}
	}

	// 过滤数据
	var filteredRows [][]string
	filteredRows = append(filteredRows, headers) // 添加表头

	rowsProcessed := len(rows) - 1 // 不包括表头
	rowsFiltered := 0

	for i := 1; i < len(rows); i++ {
		row := rows[i]
		if columnIndex >= len(row) {
			continue // 跳过列数不足的行
		}

		cellValue := strings.TrimSpace(row[columnIndex])
		exists := txtMap[cellValue]

		// 根据过滤类型决定是否保留该行
		shouldKeep := false
		if config.FilterType == "include" {
			shouldKeep = exists // 包含：保留TXT中存在的行
		} else if config.FilterType == "exclude" {
			shouldKeep = !exists // 不包含：保留TXT中不存在的行
		}

		if shouldKeep {
			filteredRows = append(filteredRows, row)
			rowsFiltered++
		}
	}

	// 创建新的Excel文件
	newFile := excelize.NewFile()
	defer newFile.Close()

	// 写入过滤后的数据
	for i, row := range filteredRows {
		for j, cellValue := range row {
			cell := getCellName(j+1, i+1)
			newFile.SetCellValue("Sheet1", cell, cellValue)
		}
	}

	// 保存文件
	if err := newFile.SaveAs(config.OutputPath); err != nil {
		return FilterResult{
			Success: false,
			Message: fmt.Sprintf("保存文件失败: %v", err),
		}
	}

	return FilterResult{
		Success:       true,
		Message:       "过滤完成！",
		OutputPath:    config.OutputPath,
		RowsProcessed: rowsProcessed,
		RowsFiltered:  rowsFiltered,
	}
}

// readTxtFile 读取TXT文件内容
func readTxtFile(filePath string) ([]string, error) {
	file, err := os.Open(filePath)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	var lines []string
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line != "" { // 跳过空行
			lines = append(lines, line)
		}
	}

	if err := scanner.Err(); err != nil {
		return nil, err
	}

	return lines, nil
}

// UpdateExcelFile 根据匹配字段更新Excel文件
func (a *App) UpdateExcelFile(config UpdateConfig) UpdateResult {
	// 验证输入参数
	if config.MainFile == "" || config.SubFile == "" || config.OutputPath == "" || config.MatchColumn == "" || len(config.UpdateColumns) == 0 {
		return UpdateResult{
			Success: false,
			Message: "请完善所有必填项",
		}
	}

	// 处理主文件路径
	mainPath := findActualPath(config.MainFile)
	if mainPath == "" {
		return UpdateResult{
			Success: false,
			Message: fmt.Sprintf("找不到主文件: %s", config.MainFile),
		}
	}

	// 处理副文件路径
	subPath := findActualPath(config.SubFile)
	if subPath == "" {
		return UpdateResult{
			Success: false,
			Message: fmt.Sprintf("找不到副文件: %s", config.SubFile),
		}
	}

	// 读取副文件数据
	subData, err := readExcelToMap(subPath, config.MatchColumn, config.UpdateColumns)
	if err != nil {
		return UpdateResult{
			Success: false,
			Message: fmt.Sprintf("读取副文件失败: %v", err),
		}
	}

	if len(subData) == 0 {
		return UpdateResult{
			Success: false,
			Message: "副文件没有有效数据",
		}
	}

	// 打开主文件
	f, err := excelize.OpenFile(mainPath)
	if err != nil {
		return UpdateResult{
			Success: false,
			Message: fmt.Sprintf("打开主文件失败: %v", err),
		}
	}
	defer f.Close()

	// 获取第一个工作表
	sheets := f.GetSheetList()
	if len(sheets) == 0 {
		return UpdateResult{
			Success: false,
			Message: "主文件为空",
		}
	}

	// 读取主文件所有行
	rows, err := f.GetRows(sheets[0])
	if err != nil {
		return UpdateResult{
			Success: false,
			Message: fmt.Sprintf("读取主文件工作表失败: %v", err),
		}
	}

	if len(rows) == 0 {
		return UpdateResult{
			Success: false,
			Message: "主文件没有数据",
		}
	}

	// 找到匹配列和更新列的索引
	headers := rows[0]
	matchColumnIndex := -1
	updateColumnIndexes := make(map[string]int)

	for i, header := range headers {
		if strings.TrimSpace(header) == strings.TrimSpace(config.MatchColumn) {
			matchColumnIndex = i
		}
		for _, updateCol := range config.UpdateColumns {
			if strings.TrimSpace(header) == strings.TrimSpace(updateCol) {
				updateColumnIndexes[updateCol] = i
			}
		}
	}

	if matchColumnIndex == -1 {
		return UpdateResult{
			Success: false,
			Message: fmt.Sprintf("主文件中未找到匹配列: %s", config.MatchColumn),
		}
	}

	// 检查是否找到了所有要更新的列
	for _, updateCol := range config.UpdateColumns {
		if _, exists := updateColumnIndexes[updateCol]; !exists {
			return UpdateResult{
				Success: false,
				Message: fmt.Sprintf("主文件中未找到更新列: %s", updateCol),
			}
		}
	}

	// 更新数据
	rowsProcessed := len(rows) - 1 // 不包括表头
	rowsUpdated := 0

	for i := 1; i < len(rows); i++ {
		row := rows[i]
		if matchColumnIndex >= len(row) {
			continue // 跳过列数不足的行
		}

		matchValue := strings.TrimSpace(row[matchColumnIndex])
		if subRowData, exists := subData[matchValue]; exists {
			// 找到匹配的行，更新指定字段
			for _, updateCol := range config.UpdateColumns {
				if colIndex, colExists := updateColumnIndexes[updateCol]; colExists {
					if newValue, valueExists := subRowData[updateCol]; valueExists {
						// 确保行有足够的列
						for len(row) <= colIndex {
							row = append(row, "")
						}
						row[colIndex] = newValue
						rows[i] = row
					}
				}
			}
			rowsUpdated++
		}
	}

	// 创建新的Excel文件
	newFile := excelize.NewFile()
	defer newFile.Close()

	// 写入更新后的数据
	for i, row := range rows {
		for j, cellValue := range row {
			cell := getCellName(j+1, i+1)
			newFile.SetCellValue("Sheet1", cell, cellValue)
		}
	}

	// 保存文件
	if err := newFile.SaveAs(config.OutputPath); err != nil {
		return UpdateResult{
			Success: false,
			Message: fmt.Sprintf("保存文件失败: %v", err),
		}
	}

	return UpdateResult{
		Success:       true,
		Message:       "文件更新完成！",
		OutputPath:    config.OutputPath,
		RowsProcessed: rowsProcessed,
		RowsUpdated:   rowsUpdated,
	}
}

// readExcelToMap 读取Excel文件并转换为以指定列为key的map
func readExcelToMap(filePath, keyColumn string, valueColumns []string) (map[string]map[string]string, error) {
	f, err := excelize.OpenFile(filePath)
	if err != nil {
		return nil, err
	}
	defer f.Close()

	sheets := f.GetSheetList()
	if len(sheets) == 0 {
		return nil, fmt.Errorf("文件为空")
	}

	rows, err := f.GetRows(sheets[0])
	if err != nil {
		return nil, err
	}

	if len(rows) == 0 {
		return nil, fmt.Errorf("工作表为空")
	}

	// 找到key列和value列的索引
	headers := rows[0]
	keyColumnIndex := -1
	valueColumnIndexes := make(map[string]int)

	for i, header := range headers {
		if strings.TrimSpace(header) == strings.TrimSpace(keyColumn) {
			keyColumnIndex = i
		}
		for _, valueCol := range valueColumns {
			if strings.TrimSpace(header) == strings.TrimSpace(valueCol) {
				valueColumnIndexes[valueCol] = i
			}
		}
	}

	if keyColumnIndex == -1 {
		return nil, fmt.Errorf("未找到匹配列: %s", keyColumn)
	}

	// 构建数据map
	result := make(map[string]map[string]string)
	for i := 1; i < len(rows); i++ {
		row := rows[i]
		if keyColumnIndex >= len(row) {
			continue
		}

		keyValue := strings.TrimSpace(row[keyColumnIndex])
		if keyValue == "" {
			continue
		}

		rowData := make(map[string]string)
		for valueCol, colIndex := range valueColumnIndexes {
			if colIndex < len(row) {
				rowData[valueCol] = strings.TrimSpace(row[colIndex])
			} else {
				rowData[valueCol] = ""
			}
		}
		result[keyValue] = rowData
	}

	return result, nil
}

// findActualPath 查找文件的实际路径
func findActualPath(filePath string) string {
	fmt.Printf("查找文件: %s\n", filePath)

	// 如果是绝对路径且文件存在，直接返回
	if filepath.IsAbs(filePath) {
		if _, err := os.Stat(filePath); err == nil {
			fmt.Printf("找到绝对路径文件: %s\n", filePath)
			return filePath
		}
	}

	// 如果只是文件名，尝试在常见目录查找
	actualPath := filePath
	if !filepath.IsAbs(filePath) {
		// 尝试在用户下载目录查找
		userHome, _ := os.UserHomeDir()
		downloadPath := filepath.Join(userHome, "Downloads", filePath)
		if _, err := os.Stat(downloadPath); err == nil {
			actualPath = downloadPath
			fmt.Printf("在下载目录找到文件: %s\n", actualPath)
			return actualPath
		}

		// 尝试在当前目录查找
		currentDir, _ := os.Getwd()
		currentPath := filepath.Join(currentDir, filePath)
		if _, err := os.Stat(currentPath); err == nil {
			actualPath = currentPath
			fmt.Printf("在当前目录找到文件: %s\n", actualPath)
			return actualPath
		}

		// 尝试在桌面查找
		desktopPath := filepath.Join(userHome, "Desktop", filePath)
		if _, err := os.Stat(desktopPath); err == nil {
			actualPath = desktopPath
			fmt.Printf("在桌面找到文件: %s\n", actualPath)
			return actualPath
		}
	}

	// 如果都找不到，返回空字符串
	fmt.Printf("未找到文件: %s\n", filePath)
	return ""
}

// readCSVFile 读取CSV文件
func readCSVFile(filePath string) ([][]string, error) {
	file, err := os.Open(filePath)
	if err != nil {
		return nil, fmt.Errorf("打开CSV文件失败: %v", err)
	}
	defer file.Close()

	reader := csv.NewReader(file)
	// 设置CSV读取选项
	reader.Comma = ','
	reader.Comment = '#'
	reader.TrimLeadingSpace = true

	records, err := reader.ReadAll()
	if err != nil {
		return nil, fmt.Errorf("读取CSV文件失败: %v", err)
	}

	// 清理BOM字符和多余空格
	for i, record := range records {
		for j, field := range record {
			// 移除BOM字符（UTF-8 BOM: \uFEFF）
			field = strings.TrimPrefix(field, "\uFEFF")
			// 移除前后空格
			field = strings.TrimSpace(field)
			records[i][j] = field
		}
	}

	return records, nil
}

// isCSVFile 检查文件是否为CSV文件
func isCSVFile(filePath string) bool {
	ext := strings.ToLower(filepath.Ext(filePath))
	return ext == ".csv"
}

// isExcelFile 检查文件是否为Excel文件
func isExcelFile(filePath string) bool {
	ext := strings.ToLower(filepath.Ext(filePath))
	return ext == ".xlsx" || ext == ".xls"
}

// ProcessAgeColumn 处理年龄列数据
func (a *App) ProcessAgeColumn(config AgeProcessConfig) AgeProcessResult {
	// 验证输入参数
	if config.ExcelFile == "" || config.OutputPath == "" || config.AgeColumn == "" {
		return AgeProcessResult{
			Success: false,
			Message: "请完善所有必填项",
		}
	}

	if config.Threshold < 0 || config.Increment <= 0 {
		return AgeProcessResult{
			Success: false,
			Message: "阈值和增量必须为正数",
		}
	}

	// 处理Excel文件路径
	excelPath := findActualPath(config.ExcelFile)
	if excelPath == "" {
		return AgeProcessResult{
			Success: false,
			Message: fmt.Sprintf("找不到Excel文件: %s", config.ExcelFile),
		}
	}

	// 打开Excel文件
	f, err := excelize.OpenFile(excelPath)
	if err != nil {
		return AgeProcessResult{
			Success: false,
			Message: fmt.Sprintf("打开Excel文件失败: %v", err),
		}
	}
	defer f.Close()

	// 获取第一个工作表
	sheets := f.GetSheetList()
	if len(sheets) == 0 {
		return AgeProcessResult{
			Success: false,
			Message: "Excel文件为空",
		}
	}

	// 读取所有行
	rows, err := f.GetRows(sheets[0])
	if err != nil {
		return AgeProcessResult{
			Success: false,
			Message: fmt.Sprintf("读取工作表失败: %v", err),
		}
	}

	if len(rows) == 0 {
		return AgeProcessResult{
			Success: false,
			Message: "工作表没有数据",
		}
	}

	// 找到年龄列的索引
	headers := rows[0]
	ageColumnIndex := -1
	for i, header := range headers {
		if strings.TrimSpace(header) == strings.TrimSpace(config.AgeColumn) {
			ageColumnIndex = i
			break
		}
	}

	if ageColumnIndex == -1 {
		return AgeProcessResult{
			Success: false,
			Message: fmt.Sprintf("未找到年龄列: %s", config.AgeColumn),
		}
	}

	// 处理数据
	rowsProcessed := len(rows) - 1 // 不包括表头
	rowsModified := 0

	for i := 1; i < len(rows); i++ {
		row := rows[i]
		if ageColumnIndex >= len(row) {
			continue // 跳过列数不足的行
		}

		ageStr := strings.TrimSpace(row[ageColumnIndex])
		if ageStr == "" {
			continue // 跳过空值
		}

		// 尝试解析年龄
		age, err := strconv.Atoi(ageStr)
		if err != nil {
			// 如果不是整数，尝试解析浮点数
			ageFloat, err := strconv.ParseFloat(ageStr, 64)
			if err != nil {
				continue // 跳过无法解析的值
			}
			age = int(ageFloat)
		}

		// 检查是否符合条件
		if age < config.Threshold {
			newAge := age + config.Increment
			// 确保行有足够的列
			for len(row) <= ageColumnIndex {
				row = append(row, "")
			}
			row[ageColumnIndex] = strconv.Itoa(newAge)
			rows[i] = row
			rowsModified++
		}
	}

	// 创建新的Excel文件
	newFile := excelize.NewFile()
	defer newFile.Close()

	// 写入处理后的数据
	for i, row := range rows {
		for j, cellValue := range row {
			cell := getCellName(j+1, i+1)
			newFile.SetCellValue("Sheet1", cell, cellValue)
		}
	}

	// 保存文件
	if err := newFile.SaveAs(config.OutputPath); err != nil {
		return AgeProcessResult{
			Success: false,
			Message: fmt.Sprintf("保存文件失败: %v", err),
		}
	}

	return AgeProcessResult{
		Success:       true,
		Message:       "年龄处理完成！",
		OutputPath:    config.OutputPath,
		RowsProcessed: rowsProcessed,
		RowsModified:  rowsModified,
	}
}

// GenerateActivityData 生成活跃数据
func (a *App) GenerateActivityData(config ActivityConfig) ActivityResult {
	// 验证输入参数
	if config.ExcelFile == "" || config.OutputPath == "" {
		return ActivityResult{
			Success: false,
			Message: "请完善所有必填项",
		}
	}

	if config.MaxDays < 0 {
		return ActivityResult{
			Success: false,
			Message: "最大天数必须为非负数",
		}
	}

	// 处理Excel文件路径
	excelPath := findActualPath(config.ExcelFile)
	if excelPath == "" {
		return ActivityResult{
			Success: false,
			Message: fmt.Sprintf("找不到Excel文件: %s", config.ExcelFile),
		}
	}

	// 打开Excel文件
	f, err := excelize.OpenFile(excelPath)
	if err != nil {
		return ActivityResult{
			Success: false,
			Message: fmt.Sprintf("打开Excel文件失败: %v", err),
		}
	}
	defer f.Close()

	// 获取第一个工作表
	sheets := f.GetSheetList()
	if len(sheets) == 0 {
		return ActivityResult{
			Success: false,
			Message: "Excel文件为空",
		}
	}

	// 读取所有行
	rows, err := f.GetRows(sheets[0])
	if err != nil {
		return ActivityResult{
			Success: false,
			Message: fmt.Sprintf("读取工作表失败: %v", err),
		}
	}

	if len(rows) == 0 {
		return ActivityResult{
			Success: false,
			Message: "工作表没有数据",
		}
	}

	// 创建新的Excel文件
	newFile := excelize.NewFile()
	defer newFile.Close()

	// 获取当前时间
	now := time.Now()

	// 初始化随机数生成器
	rand.Seed(time.Now().UnixNano())

	// 处理表头，添加新列
	headers := rows[0]
	newHeaders := make([]string, len(headers)+2)
	copy(newHeaders, headers)
	newHeaders[len(headers)] = "活跃时间"
	newHeaders[len(headers)+1] = "天数"

	// 写入表头
	for j, header := range newHeaders {
		cell := getCellName(j+1, 1)
		newFile.SetCellValue("Sheet1", cell, header)
	}

	// 处理数据行
	rowsProcessed := len(rows) - 1 // 不包括表头

	for i := 1; i < len(rows); i++ {
		row := rows[i]
		newRow := make([]string, len(newHeaders))

		// 复制原有数据
		for j := 0; j < len(row) && j < len(headers); j++ {
			newRow[j] = row[j]
		}

		// 生成随机天数 (0 到 maxDays)
		randomDays := 0
		if config.MaxDays > 0 {
			randomDays = rand.Intn(config.MaxDays + 1)
		}

		// 生成活跃时间
		// 如果天数是N，则时间在 (今天-N-1天) 到 (今天-N天) 之间
		var activityTime time.Time
		if randomDays == 0 {
			// 天数为0，活跃时间在今天往前不超过1天
			minTime := now.AddDate(0, 0, -1)
			maxTime := now
			timeDiff := maxTime.Sub(minTime)
			randomDuration := time.Duration(rand.Int63n(int64(timeDiff)))
			activityTime = minTime.Add(randomDuration)
		} else {
			// 天数为N，活跃时间在 (今天-N-1天) 到 (今天-N天) 之间
			minTime := now.AddDate(0, 0, -(randomDays + 1))
			maxTime := now.AddDate(0, 0, -randomDays)
			timeDiff := maxTime.Sub(minTime)
			randomDuration := time.Duration(rand.Int63n(int64(timeDiff)))
			activityTime = minTime.Add(randomDuration)
		}

		// 格式化时间为 "2025-09-19 20:47:59" 格式
		activityTimeStr := activityTime.Format("2006-01-02 15:04:05")

		// 设置活跃时间和天数
		newRow[len(headers)] = activityTimeStr
		newRow[len(headers)+1] = strconv.Itoa(randomDays) // 这里先设置为字符串，但下面会特殊处理

		// 写入新行
		for j, cellValue := range newRow {
			cell := getCellName(j+1, i+1)
			if j == len(headers)+1 {
				// 天数列写入为数字类型
				newFile.SetCellValue("Sheet1", cell, randomDays)
			} else {
				// 其他列写入为字符串
				newFile.SetCellValue("Sheet1", cell, cellValue)
			}
		}
	}

	// 保存文件
	if err := newFile.SaveAs(config.OutputPath); err != nil {
		return ActivityResult{
			Success: false,
			Message: fmt.Sprintf("保存文件失败: %v", err),
		}
	}

	return ActivityResult{
		Success:       true,
		Message:       "活跃数据生成完成！",
		OutputPath:    config.OutputPath,
		RowsProcessed: rowsProcessed,
	}
}
