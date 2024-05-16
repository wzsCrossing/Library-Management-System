<template>
    <el-scrollbar height="100%" style="width: 100%;">

        <!-- 标题 -->
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold;">
            图书管理
        </div>

        <!-- 查询框 -->
        <div style="width:75%; margin:0 auto; padding-top:5vh;">

            <el-input v-model="this.QueryTitle" style="display:inline;" placeholder="书名"></el-input>
            <el-input v-model="this.QueryCategory" style="display:inline; margin-left: 19px;" placeholder="类别"></el-input>
            <el-input v-model="this.QueryPress" style="display:inline; margin-left: 19px;" placeholder="出版社"></el-input>
            <el-input v-model="this.QueryAuthor" style="display:inline; margin-left: 19px;" placeholder="作者"></el-input>
            <el-button style="margin-left: 20px;" type="primary" @click="QueryBooks">查询</el-button>
            <el-button type="success" @click="this.newBookVisible = true, this.newBookError = false, this.fileList = [],
                this.newBookInfo.type = '', this.newBookInfo.category = '', this.newBookInfo.title = '',
                this.newBookInfo.press = '', this.newBookInfo.publishYear = '', this.newBookInfo.author = '',
                this.newBookInfo.price = '', this.newBookInfo.stock = ''">
                添加
            </el-button>
        </div>

        <div style="width:75%; margin:0 auto; padding-top:1vh;">

            <el-input v-model="this.QueryMinYear" style="display:inline;" placeholder="最小出版年份"></el-input>
            ~
            <el-input v-model="this.QueryMaxYear" style="display:inline;" placeholder="最大出版年份"></el-input>
            <el-input v-model="this.QueryMinPrice" style="display:inline; margin-left: 19px;" placeholder="最小价格"></el-input>
            ~
            <el-input v-model="this.QueryMaxPrice" style="display:inline;" placeholder="最大价格"></el-input>

        </div>

        <!-- 结果表格 -->
        <el-table :data="filterTableData" height="600"
            :default-sort="{ prop: 'bookId', order: 'ascending' }" :table-layout="'auto'"
            style="width: 100%; margin-left: 50px; margin-top: 30px; margin-right: 50px; max-width: 80vw;">
            <el-table-column prop="bookId" label="图书编号" sortable align="center" />
            <el-table-column prop="title" label="书名" sortable align="center" />
            <el-table-column prop="category" label="类别" sortable align="center" />
            <el-table-column prop="press" label="出版社" sortable align="center" />
            <el-table-column prop="publishYear" label="年份" sortable align="center" />
            <el-table-column prop="author" label="作者" sortable align="center" />
            <el-table-column prop="price" label="价格" sortable align="center" />
            <el-table-column prop="stock" label="库存" sortable align="center" />

            <el-table-column label="操作" align="center">
                <template #default="{ row }">
                    <el-button type="success" circle @click="this.BorrowInfo.bookId = row.bookId, this.BorrowInfo.cardId = '',
                        this.BorrowInfo.stock = row.stock, this.BorrowBookVisible = true,
                        this.BorrowError1 = false, this.BorrowError2 = false">
                        借
                    </el-button>
                    <el-button type="warning" circle @click="this.ReturnInfo.bookId = row.bookId, this.ReturnInfo.cardId = '',
                        this.ReturnBookVisible = true, this.ReturnError1 = false, this.ReturnError2 = false">
                        还 
                    </el-button>
                    <el-button type="primary" :icon="Edit" circle @click="this.ModifyInfo.type = '', this.ModifyInfo.bookId = row.bookId,
                        this.ModifyInfo.category = row.category, this.ModifyInfo.title = row.title, this.ModifyInfo.press = row.press,
                        this.ModifyInfo.publishYear = row.publishYear, this.ModifyInfo.author = row.author, this.ModifyInfo.price = row.price,
                        this.ModifyInfo.oldStock = row.stock, this.ModifyInfo.newStock = 0,
                        this.modifyBookVisible = true, this.ModifyError1 = false, this.ModifyError2 = false" />
                    <el-button type="danger" :icon="Delete" circle 
                        @click="this.toRemove = row.bookId, this.removeBookVisible = true, this.removeBookError = false" />
                </template>
            </el-table-column>
        </el-table>

        <!-- 删除图书对话框 -->  
        <el-dialog v-model="removeBookVisible" title="删除图书" width="30%">
            <span>确定删除<span style="font-weight: bold;">{{ toRemove }}号图书</span>吗？</span>

            <div style=" font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                <el-alert title="删除失败：还有人尚未归还这本书" type="error" v-show="removeBookError" style="width: 15vw;" :closable="false" />
            </div>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="removeBookVisible = false">取消</el-button>
                    <el-button type="danger" @click="ConfirmRemoveBook">
                        删除
                    </el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 借书对话框 -->   
        <el-dialog v-model="BorrowBookVisible" :title="'借书（图书编号: ' + this.BorrowInfo.bookId + '，剩余库存: ' + this.BorrowInfo.stock + '）'" width="30%"
            align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                借书证编号：
                <el-input v-model="BorrowInfo.cardId" style="width: 12.5vw;" clearable />
            </div>

            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                <el-alert title="无法借阅：库存不足！" type="error" v-show="BorrowInfo.stock === 0" style="width: 15vw;" :closable="false" />
            </div>
            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                <el-alert title="错误：借书证不存在！" type="error" v-show="BorrowError1" style="width: 15vw;" :closable="false" />
            </div>
            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                <el-alert title="借书失败：该用户已经借过这本书且尚未归还" type="error" v-show="BorrowError2" style="width: 19vw;" :closable="false" />
            </div>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="BorrowBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="BorrowBook"
                        :disabled="BorrowInfo.cardId.length === 0 || BorrowInfo.stock === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 还书对话框 -->   
        <el-dialog v-model="ReturnBookVisible" :title="'还书（图书编号: ' + this.ReturnInfo.bookId + '）'" width="30%"
            align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                借书证编号：
                <el-input v-model="ReturnInfo.cardId" style="width: 12.5vw;" clearable />
            </div>

            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                <el-alert title="错误：借书证不存在！" type="error" v-show="ReturnError1" style="width: 15vw;" :closable="false" />
            </div>
            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                <el-alert title="还书失败：该用户没有对应的借书记录" type="error" v-show="ReturnError2" style="width: 19vw;" :closable="false" />
            </div>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="ReturnBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ReturnBook"
                        :disabled="ReturnInfo.cardId.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 编辑图书对话框 -->   
        <el-dialog v-model="modifyBookVisible" :title="'编辑（图书编号: ' + this.ModifyInfo.bookId + '）'" width="30%"
            align-center>
            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                编辑类型：
                <el-select v-model="ModifyInfo.type" size="middle" style="width: 12.5vw;">
                    <el-option v-for="type in ModifyTypes" :key="type.value" :label="type.label" :value="type.value" />
                </el-select>
            </div>
            <div style="margin-left: 3.9vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="ModifyInfo.type == 'info'">
                书名：
                <el-input v-model="ModifyInfo.title" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 3.9vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="ModifyInfo.type == 'info'">
                类别：
                <el-input v-model="ModifyInfo.category" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2.9vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="ModifyInfo.type == 'info'">
                出版社：
                <el-input v-model="ModifyInfo.press" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="ModifyInfo.type == 'info'">
                出版年份：
                <el-input v-model.number="ModifyInfo.publishYear" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 3.9vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="ModifyInfo.type == 'info'">
                作者：
                <el-input v-model="ModifyInfo.author" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 3.9vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="ModifyInfo.type == 'info'">
                价格：
                <el-input v-model.number="ModifyInfo.price" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2.9vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="ModifyInfo.type == 'stock'">
                原库存：&nbsp; &nbsp;{{ ModifyInfo.oldStock }}
            </div>
            <div style="margin-left: 2.9vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="ModifyInfo.type == 'stock'">
                新库存：
                <el-input v-model.number="ModifyInfo.newStock" style="width: 12.5vw;" clearable />
            </div>

            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                <el-alert title="修改失败：与其他图书信息重合" type="error" v-show="ModifyInfo.type == 'info' && ModifyError1" style="width: 15vw;" :closable="false" />
            </div>
            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                <el-alert title="修改失败：非法库存量" type="error" v-show="ModifyInfo.type == 'stock' && ModifyError2" style="width: 15vw;" :closable="false" />
            </div>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="modifyBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ModifyInfo.type == 'info' ? ModifyBookInfo() : ModifyBookStock()"
                        :disabled="(ModifyInfo.type.length === 0) || (ModifyInfo.type == 'info' && (ModifyInfo.title.length === 0 || ModifyInfo.category.length === 0 ||
                        ModifyInfo.press.length === 0 || ModifyInfo.publishYear.length === 0 || ModifyInfo.author.length === 0 || ModifyInfo.price.length === 0)) || 
                        (ModifyInfo.type == 'stock' && ModifyInfo.newStock.length === 0)">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 添加图书对话框 -->   
        <el-dialog v-model="newBookVisible" :title="'图书入库'" width="30%"
            align-center>
            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                添加方式：
                <el-select v-model="newBookInfo.type" size="middle" style="width: 12.5vw;">
                    <el-option v-for="type in newBookTypes" :key="type.value" :label="type.label" :value="type.value" />
                </el-select>
            </div>
            <div style="margin-left: 3.9vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="newBookInfo.type == 'store'">
                书名：
                <el-input v-model="newBookInfo.title" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 3.9vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="newBookInfo.type == 'store'">
                类别：
                <el-input v-model="newBookInfo.category" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2.9vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="newBookInfo.type == 'store'">
                出版社：
                <el-input v-model="newBookInfo.press" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="newBookInfo.type == 'store'">
                出版年份：
                <el-input v-model.number="newBookInfo.publishYear" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 3.9vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="newBookInfo.type == 'store'">
                作者：
                <el-input v-model="newBookInfo.author" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 3.9vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="newBookInfo.type == 'store'">
                价格：
                <el-input v-model.number="newBookInfo.price" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="newBookInfo.type == 'store'">
                初始库存：
                <el-input v-model.number="newBookInfo.stock" style="width: 12.5vw;" clearable />
            </div>

            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; " v-show="newBookInfo.type == 'bulkStore'">
                <el-upload
                    ref="upload"
                    accept=".txt"
                    action="#"
                    :limit="1"
                    :auto-upload="false"
                    :file-list="fileList"
                    :on-change="handleChange"
                    :on-exceed="limitCheck"
                    >
                    <span style="font-weight: bold; font-size: 1rem;">文件路径：</span> 
                    <el-button size="mini" type="primary" style="margin-left: 0.3vw;">选择文件</el-button>
                    <div slot="tip" class="el-upload__tip" style="margin-left: 1.5vw;">
                        <p>1. 只支持 .txt 文件</p>
                        <p>2. 最多上传1个文件</p>
                    </div>
                </el-upload>
            </div>

            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                <el-alert title="添加失败：与已有图书信息重合" type="error" v-show="newBookError" style="width: 15vw;" :closable="false" />
            </div>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="newBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="newBookInfo.type == 'store' ? storeBook() : bulkStoreBook()"
                        :disabled="(newBookInfo.type.length === 0) || (newBookInfo.type == 'store' && (newBookInfo.title.length === 0 || newBookInfo.category.length === 0 ||
                        newBookInfo.press.length === 0 || newBookInfo.publishYear.length === 0 || newBookInfo.author.length === 0 || newBookInfo.price.length === 0 || newBookInfo.stock.length === 0)) || 
                        (newBookInfo.type == 'bulkStore' && fileList.length === 0)">确定</el-button>
                </span>
            </template>
        </el-dialog>

    </el-scrollbar>
</template>

<script>
import axios from 'axios';
import { ElMessage } from 'element-plus';
import { Search, Edit, Delete } from '@element-plus/icons-vue'

export default {
    data() {
        return {
            fileList: [],
            fileContent: '',
            books: [{ 
                bookId: 1,
                category: 'History',
                title: 'The Old Man and the Sea',
                press: 'Press-C',
                publishYear: 2020,
                author: 'DouDou',
                price: 127.78,
                stock: 71
            }, {
                bookId: 2,
                category: 'Novel',
                title: 'Analysis of Dreams',
                press: 'Press-H',
                publishYear: 2016,
                author: 'Immortal',
                price: 2.87,
                stock: 8
            }],
            QueryTitle: '',
            QueryCategory: '',
            QueryPress: '',
            QueryAuthor: '',
            QueryMinYear: '',
            QueryMaxYear: '',
            QueryMinPrice: '',
            QueryMaxPrice: '',
            toRemove: 0,
            BorrowBookVisible: false,
            BorrowError1: false,
            BorrowError2: false,
            BorrowInfo: {
                bookId: 0,
                stock: 0,
                cardId: ''
            },
            ReturnBookVisible: false,
            ReturnError1: false,
            ReturnError2: false,
            ReturnInfo: {
                bookId: 0,
                cardId: ''
            },
            modifyBookVisible: false,
            ModifyError1: false,
            ModifyError2: false,
            ModifyTypes: [{
                value: 'info',
                label: '修改基本信息'
            }, {
                value: 'stock',
                label: '更新库存'
            }],
            ModifyInfo: {
                type: '',
                bookId: 0,
                category: '',
                title: '',
                press: '',
                publishYear: 0,
                author: '',
                price: 0.0,
                oldStock: 0,
                newStock: 0
            },
            newBookVisible: false,
            newBookError: false,
            newBookTypes: [{
                value: 'store',
                label: '单书入库'
            }, {
                value: 'bulkStore',
                label: '批量入库'
            }],
            newBookInfo: {
                type: '',
                category: '',
                title: '',
                press: '',
                publishYear: 0,
                author: '',
                price: 0.0,
                stock: 0
            },
            removeBookError: false,
            removeBookVisible: false,
            Search,
            Edit,
            Delete
        }
    },
    computed: {
        filterTableData() {
            return this.books.filter(
                (tuple) =>
                    tuple.title.includes(this.QueryTitle) &&
                    (this.QueryCategory == '' || tuple.category == this.QueryCategory) &&
                    tuple.press.includes(this.QueryPress) &&
                    tuple.author.includes(this.QueryAuthor) &&
                    (this.QueryMinYear == '' || tuple.publishYear >= parseInt(this.QueryMinYear)) &&
                    (this.QueryMaxYear == '' || tuple.publishYear <= parseInt(this.QueryMaxYear)) &&
                    (this.QueryMinPrice == '' || tuple.price >= parseFloat(this.QueryMinPrice)) &&
                    (this.QueryMaxPrice == '' || tuple.price <= parseFloat(this.QueryMaxPrice))
            )
        }
    },
    methods: {
        handleChange(file, fileList) {
            this.fileList = fileList
        },
        limitCheck() {
            this.$message.warning('文件超出个数限制')
        },
        QueryBooks() {
            this.books = []
            let response = axios.get('/book')
                .then(response => {
                    let books = response.data
                    books.forEach(book => {
                        this.books.push(book)
                    })
                })
        },
        storeBook() {
            axios.post("/book",
                {
                    type: "store",
                    title: this.newBookInfo.title,
                    category: this.newBookInfo.category,
                    press: this.newBookInfo.press,
                    publishYear: this.newBookInfo.publishYear,
                    author: this.newBookInfo.author,
                    price: this.newBookInfo.price,
                    stock: this.newBookInfo.stock
                })
                .then(response => {
                    this.newBookError = false
                    let content = response.data
                    if (content == 'Success') {
                        ElMessage.success("图书入库成功") // 显示消息提醒
                        this.newBookVisible = false // 将对话框设置为不可见
                        this.QueryBooks() // 重新查询图书
                    } else {
                        this.newBookError = true
                    }
                })
        },
        bulkStoreBook() {
            const reader = new FileReader()
            reader.onload = e => {
                this.fileContent = e.target.result
                axios.post("/book",
                    {
                        type: "bulkStore",
                        content: this.fileContent
                    })
                    .then(response => {
                        this.newBookError = false
                        let content = response.data
                        if (content == 'Success') {
                            ElMessage.success("图书入库成功") // 显示消息提醒
                            this.newBookVisible = false // 将对话框设置为不可见
                            this.QueryBooks() // 重新查询图书
                        } else {
                            this.newBookError = true
                        }
                    })
            }
            reader.readAsText(this.fileList[0].raw)
        },
        BorrowBook() {
            axios.put("/book",
                {
                    type: "borrow",
                    bookId: this.BorrowInfo.bookId,
                    cardId: this.BorrowInfo.cardId     
                })
                .then(response => {
                    this.BorrowError1 = false
                    this.BorrowError2 = false
                    let content = response.data
                    if (content == 'Success') {
                        ElMessage.success("借书成功") // 显示消息提醒
                        this.BorrowBookVisible = false // 将对话框设置为不可见
                        this.QueryBooks() // 重新查询图书
                    } else if (content == 'Non-exist card.') {
                        this.BorrowError1 = true
                    } else {
                        this.BorrowError2 = true
                    }
                })
        },
        ReturnBook() {
            axios.put("/book",
                {
                    type: "return",
                    bookId: this.ReturnInfo.bookId,
                    cardId: this.ReturnInfo.cardId
                })
                .then(response => {
                    this.ReturnError1 = false
                    this.ReturnError2 = false
                    let content = response.data
                    if (content == 'Success') {
                        ElMessage.success("还书成功") // 显示消息提醒
                        this.ReturnBookVisible = false // 将对话框设置为不可见
                        this.QueryBooks() // 重新查询图书
                    } else if (content == 'Non-exist card.') {
                        this.ReturnError1 = true
                    } else {
                        this.ReturnError2 = true
                    }
                })
        },
        ModifyBookInfo() {
            axios.put("/book",
                {
                    type: "info",
                    bookId: this.ModifyInfo.bookId,
                    category: this.ModifyInfo.category,
                    title: this.ModifyInfo.title,
                    press: this.ModifyInfo.press,
                    publishYear: this.ModifyInfo.publishYear,
                    author: this.ModifyInfo.author,
                    price: this.ModifyInfo.price
                })
                .then(response => {
                    this.ModifyError1 = false
                    this.ModifyError2 = false
                    let content = response.data
                    if (content == 'Success') {
                        ElMessage.success("图书信息修改成功") // 显示消息提醒
                        this.modifyBookVisible = false // 将对话框设置为不可见
                        this.QueryBooks() // 重新查询图书
                    } else {
                        this.ModifyError1 = true
                    }
                })
        },
        ModifyBookStock() {
            axios.put("/book",
                {
                    type: "stock",
                    bookId: this.ModifyInfo.bookId,
                    deltaStock: this.ModifyInfo.newStock - this.ModifyInfo.oldStock
                })
                .then(response => {
                    this.ModifyError1 = false
                    this.ModifyError2 = false
                    let content = response.data
                    if (content == 'Success') {
                        ElMessage.success("库存更新成功") // 显示消息提醒
                        this.modifyBookVisible = false // 将对话框设置为不可见
                        this.QueryBooks() // 重新查询图书
                    } else {
                        this.ModifyError2 = true
                    }
                })
        },
        ConfirmRemoveBook() {
            axios.delete('/book', { params: { bookId: this.toRemove } })
                .then(response => {
                    let content = response.data
                    if (content == 'Success') {
                        ElMessage.success("图书删除成功") // 显示消息提醒
                        this.removeBookVisible = false // 将对话框设置为不可见
                        this.QueryBooks() // 重新查询图书
                    } else {
                        this.removeBookError = true
                    }
                })
        }
    },
    mounted() {
        this.QueryBooks()
    }
}
</script>