document.addEventListener('DOMContentLoaded', loadUsers);

function loadUsers() {
    fetch('/api/admin/users')
        .then(res => res.json())
        .then(data => {
            if (!data.success) {
                document.getElementById('userTableBody').innerHTML =
                    '<tr><td colspan="7" class="text-center text-danger">載入失敗</td></tr>';
                return;
            }
            const users = data.data || [];
            if (users.length === 0) {
                document.getElementById('userTableBody').innerHTML =
                    '<tr><td colspan="7" class="text-center text-muted py-4">尚無使用者資料</td></tr>';
                return;
            }
            const tbody = document.getElementById('userTableBody');
            tbody.innerHTML = '';
            users.forEach(u => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${u.id}</td>
                    <td>${u.username}</td>
                    <td>${u.email}</td>
                    <td class="text-center">
                        <span class="badge ${u.emailVerified ? 'bg-success' : 'bg-warning text-dark'}">
                            ${u.emailVerified ? '已驗證' : '未驗證'}
                        </span>
                    </td>
                    <td class="text-center">
                        <span class="badge ${u.status === 'ACTIVE' ? 'bg-success' : 'bg-secondary'}">
                            ${u.status}
                        </span>
                    </td>
                    <td class="text-center">
                        ${u.systemAdmin ? '<span class="badge bg-danger">ADMIN</span>' : '-'}
                    </td>
                    <td class="small text-muted">${u.createdAt ? u.createdAt.substring(0, 10) : '-'}</td>
                `;
                tbody.appendChild(tr);
            });
        })
        .catch(() => {
            document.getElementById('userTableBody').innerHTML =
                '<tr><td colspan="7" class="text-center text-danger">網路錯誤</td></tr>';
        });
}
