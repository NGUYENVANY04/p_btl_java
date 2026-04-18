

// // Role-based functions
// function updateRoleDisplay() {
//     const userInfo = document.querySelector('.user-info');
//     const roleBadge = document.createElement('div');
//     roleBadge.className = `role-badge role-${currentUser.role}`;
//     roleBadge.textContent = currentUser.role === 'admin' ? 'Quản trị viên' : 'Người dùng';

//     // Remove existing role badge if any
//     const existingBadge = userInfo.querySelector('.role-badge');
//     if (existingBadge) existingBadge.remove();

//     userInfo.appendChild(roleBadge);
// }

// function applyRolePermissions() {
//     const navItems = document.querySelectorAll('.nav-button, .submenu button');

//     navItems.forEach(item => {
//         const url = item.getAttribute('onclick')?.match(/changePage\('([^']+)'/)?.[1];
//         if (url) {
//             const hasPermission = checkPermission(url);
//             item.style.display = hasPermission ? 'flex' : 'none';
//         }
//     });
// }

// function checkPermission(url) {
//     if (!currentUser || currentUser.role === 'admin') return true;

//     const permissions = currentUser.permissions || [];

//     // Map URLs to permissions
//     const permissionMap = {
//         'home': 'view_dashboard',
//         'usercase2/usercase2.html': 'view_sensors',
//         'budget_manager/budget.html': 'view_budget',
//         'usercase4/usercase4.html': 'view_notifications',
//         'usercase1/device.html': 'manage_devices',
//         'usercase1/threshold.html': 'manage_thresholds',
//         'usercase3/usercase3.html': 'manage_network',
//         'usercase5/usercase5.html': 'manage_accounts'
//     };

//     const requiredPermission = permissionMap[url];
//     return requiredPermission ? permissions.includes(requiredPermission) : false;
// }