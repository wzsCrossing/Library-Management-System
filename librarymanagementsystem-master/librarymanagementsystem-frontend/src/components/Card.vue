<template>
    <el-scrollbar height="100%" style="width: 100%;">
        <!-- 标题和搜索框 -->
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">借书证管理
            <el-input v-model="toSearch" :prefix-icon="Search"
                style=" width: 15vw;min-width: 150px; margin-left: 30px; margin-right: 30px; float: right;" clearable />
        </div>

        <!-- 借书证卡片显示区 -->
        <div style="display: flex;flex-wrap: wrap; justify-content: start;">

            <!-- 借书证卡片 -->
            <div class="cardBox" v-for="card in cards" v-show="card.name.includes(toSearch)" :key="card.cardId">
                <div>
                    <!-- 卡片标题 -->
                    <div style="font-size: 25px; font-weight: bold;">No. {{ card.cardId }}</div>

                    <el-divider />

                    <!-- 卡片内容 -->
                    <div style="margin-left: 10px; text-align: start; font-size: 16px;">
                        <p style="padding: 2.5px;"><span style="font-weight: bold;">姓名：</span>{{ card.name }}</p>
                        <p style="padding: 2.5px;"><span style="font-weight: bold;">部门：</span>{{ card.department }}</p>
                        <p style="padding: 2.5px;"><span style="font-weight: bold;">类型：</span>{{ card.type }}</p>
                    </div>

                    <el-divider />

                    <!-- 卡片操作 -->
                    <div style="margin-top: 10px;">
                        <el-button type="primary" :icon="Edit" @click="this.toModifyInfo.cardId = card.cardId, this.toModifyInfo.name = card.name,
                this.toModifyInfo.department = card.department, this.toModifyInfo.type = card.type,
                this.modifyCardVisible = true, this.modifyCardError = false" circle />
                        <el-button type="danger" :icon="Delete" circle
                            @click="this.toRemove = card.cardId, this.removeCardVisible = true, this.removeCardError = false"
                            style="margin-left: 30px;" />
                    </div>

                </div>
            </div>

            <!-- 新建借书证卡片 -->
            <el-button class="newCardBox"
                @click="newCardInfo.name = '', newCardInfo.department = '', newCardInfo.type = '学生', newCardVisible = true, newCardError = false">
                <el-icon style="height: 50px; width: 50px;">
                    <Plus style="height: 100%; width: 100%;" />
                </el-icon>
            </el-button>

        </div>


        <!-- 新建借书证对话框 -->
        <el-dialog v-model="newCardVisible" title="新建借书证" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                姓名：
                <el-input v-model="newCardInfo.name" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                部门：
                <el-input v-model="newCardInfo.department" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类型：
                <el-select v-model="newCardInfo.type" size="middle" style="width: 12.5vw;">
                    <el-option v-for="type in types" :key="type.value" :label="type.label" :value="type.value" />
                </el-select>
            </div>

            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                <el-alert title="添加失败：借书证已存在" type="error" v-show="newCardError" style="width: 15vw;" :closable="false" />
            </div>
            
            <template #footer>
                <span>
                    <el-button @click="newCardVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmNewCard"
                        :disabled="newCardInfo.name.length === 0 || newCardInfo.department.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>


        <!-- 修改信息对话框 -->   
        <el-dialog v-model="modifyCardVisible" :title="'修改信息(借书证ID: ' + this.toModifyInfo.cardId + ')'" width="30%"
            align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                姓名：
                <el-input v-model="toModifyInfo.name" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                部门：
                <el-input v-model="toModifyInfo.department" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类型：
                <el-select v-model="toModifyInfo.type" size="middle" style="width: 12.5vw;">
                    <el-option v-for="type in types" :key="type.value" :label="type.label" :value="type.value" />
                </el-select>
            </div>

            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                <el-alert title="修改失败：与其他借书证信息重合" type="error" v-show="modifyCardError" style="width: 15vw;" :closable="false" />
            </div>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="modifyCardVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmModifyCard"
                        :disabled="toModifyInfo.name.length === 0 || toModifyInfo.department.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 删除借书证对话框 -->  
        <el-dialog v-model="removeCardVisible" title="删除借书证" width="30%">
            <span>确定删除<span style="font-weight: bold;">{{ toRemove }}号借书证</span>吗？</span>

            <div style=" font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                <el-alert title="删除失败：该借书证还有未归还的图书" type="error" v-show="removeCardError" style="width: 17vw;" :closable="false" />
            </div>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="removeCardVisible = false">取消</el-button>
                    <el-button type="danger" @click="ConfirmRemoveCard">
                        删除
                    </el-button>
                </span>
            </template>
        </el-dialog>

    </el-scrollbar>
</template>

<script>
import { Delete, Edit, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

export default {
    data() {
        return {
            cards: [{ // 借书证列表
                cardId: 1,
                name: '小明',
                department: '计算机学院',
                type: '学生'
            }, {
                cardId: 2,
                name: '王老师',
                department: '计算机学院',
                type: '教师'
            }
            ],
            Delete,
            Edit,
            Search,
            toSearch: '', // 搜索内容
            types: [ // 借书证类型
                {
                    value: '教师',
                    label: '教师',
                },
                {
                    value: '学生',
                    label: '学生',
                }
            ],
            newCardVisible: false, // 新建借书证对话框可见性
            removeCardVisible: false, // 删除借书证对话框可见性
            toRemove: 0, // 待删除借书证号
            newCardInfo: { // 待新建借书证信息
                name: '',
                department: '',
                type: '学生'
            },
            newCardError: false,
            removeCardError: false,
            modifyCardError: false,
            modifyCardVisible: false, // 修改信息对话框可见性
            toModifyInfo: { // 待修改借书证信息
                cardId: 0,
                name: '',
                department: '',
                type: '学生'
            },
        }
    },
    methods: {
        ConfirmNewCard() {
            // 发出POST请求
            axios.post("/card",
                { // 请求体
                    name: this.newCardInfo.name,
                    department: this.newCardInfo.department,
                    type: this.newCardInfo.type
                })
                .then(response => {
                    let content = response.data
                    if (content == "Success") {
                        ElMessage.success("借书证新建成功") // 显示消息提醒
                        this.newCardVisible = false // 将对话框设置为不可见
                        this.QueryCards() // 重新查询借书证以刷新页面
                    } else {
                        this.newCardError = true
                    }
                })
        },
        ConfirmModifyCard() {
            // 发出PUT请求
            axios.put("/card",
                { // 请求体
                    cardId: this.toModifyInfo.cardId,
                    name: this.toModifyInfo.name,
                    department: this.toModifyInfo.department,
                    type: this.toModifyInfo.type
                })
                .then(response => {
                    let content = response.data
                    if (content == "Success") {
                        ElMessage.success("借书证信息修改成功") // 显示消息提醒
                        this.modifyCardVisible = false // 将对话框设置为不可见
                        this.QueryCards() // 重新查询借书证以刷新页面
                    } else {
                        this.modifyCardError = true
                    }
                })
        },
        ConfirmRemoveCard() {
            axios.delete('/card', { params: { cardId: this.toRemove } })
                .then(response => {
                    let content = response.data
                    if (content == "Success") {
                        ElMessage.success("借书证删除成功") // 显示消息提醒
                        this.removeCardVisible = false // 将对话框设置为不可见
                        this.QueryCards() // 重新查询借书证以刷新页面
                    } else {
                        this.removeCardError = true
                    }
                })
        },
        QueryCards() {
            this.cards = [] // 清空列表
            let response = axios.get('/card') // 向/card发出GET请求
                .then(response => {
                    let cards = response.data // 接收响应负载
                    cards.forEach(card => { // 对于每个借书证
                        this.cards.push(card) // 将其加入到列表中
                    })
                })
        }
    },
    mounted() { // 当页面被渲染时
        this.QueryCards() // 查询借书证
    }
}

</script>


<style scoped>
.cardBox {
    height: 300px;
    width: 250px;
    box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
    text-align: center;
    margin-top: 40px;
    margin-left: 27.5px;
    margin-right: 10px;
    padding: 7.5px;
    padding-right: 10px;
    padding-top: 15px;
}

.newCardBox {
    height: 300px;
    width: 250px;
    margin-top: 40px;
    margin-left: 27.5px;
    margin-right: 10px;
    padding: 7.5px;
    padding-right: 10px;
    padding-top: 15px;
    box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
    text-align: center;
}
</style>